package p4nic
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import common._
import common.storage._
import common.axi._
import common.ToZero
import cmac._
import qdma._
import hbm._

class WorkerNICTopnew extends RawModule {
    // I/O ports

    // Now it just hangs.
    val hbmCattrip		= IO(Output(UInt(1.W)))
    // Board system clocks.
    val sysClkP			= IO(Input(Clock()))
    val sysClkN			= IO(Input(Clock()))
    // Pins, including gt clocks.
    val cmacPin			= IO(new CMACPin())
    val qdmaPin			= IO(new QDMAPin())

    hbmCattrip := 0.U


	val mmcmTop	= Module(new MMCME4_ADV_Wrapper(
		CLKFBOUT_MULT_F 		= 10,
		MMCM_DIVCLK_DIVIDE		= 1,
		MMCM_CLKOUT0_DIVIDE_F	= 10,
		MMCM_CLKOUT1_DIVIDE_F	= 4,
		MMCM_CLKOUT2_DIVIDE_F	= 10,
		MMCM_CLKOUT3_DIVIDE_F	= 4,
		MMCM_CLKIN1_PERIOD 		= 10
	))

	mmcmTop.io.CLKIN1	:= IBUFDS(sysClkP,sysClkN)
	mmcmTop.io.RST		:= 0.U

	val clk_hbm_driver	= mmcmTop.io.CLKOUT0 //100M
	val cmacClk			= mmcmTop.io.CLKOUT2 //100M
	val netClk			= mmcmTop.io.CLKOUT3 //250M

	//HBM
    val hbmDriver 		= withClockAndReset(clk_hbm_driver, false.B) {Module(new HBM_DRIVER(WITH_RAMA=false))}
	hbmDriver.getTCL()
	val hbmClk 	    	= hbmDriver.io.hbm_clk
	val hbmRstn     	= withClockAndReset(hbmClk,false.B) {RegNext(hbmDriver.io.hbm_rstn.asBool)}
    

	for (i <- 0 until 32) {
		hbmDriver.io.axi_hbm(i).hbm_init()	// Read hbm_init function if you're not familiar with AXI.
	}

	dontTouch(hbmClk)
	dontTouch(hbmRstn)

    val eng_hbm = (Module(new AXISToHbm()))


	val userClk  	    = Wire(Clock())
	val userRstn 	    = Wire(Bool())
    val userRstn_pre 	= Wire(Bool())




    val cmacInst = Module(new XCMAC())
    cmacInst.getTCL()

    cmacPin		<> cmacInst.io.pin
    cmacInst.io.drp_clk         := cmacClk
    cmacInst.io.user_clk	    := userClk
    cmacInst.io.user_arstn	    := userRstn
    cmacInst.io.sys_reset 		<> !userRstn 

    val qdmaInst = Module(new QDMA(
        VIVADO_VERSION		= "202101",
        PCIE_WIDTH			= 16,
        SLAVE_BRIDGE		= true,
        BRIDGE_BAR_SCALE	= "Megabytes",
        BRIDGE_BAR_SIZE 	= 4
    ))
    qdmaInst.getTCL()

    // Connect QDMA's pins
    val controlReg  = qdmaInst.io.reg_control
    val statusReg   = qdmaInst.io.reg_status
    ToZero(qdmaInst.io.reg_status)

    userClk		    := qdmaInst.io.pcie_clk
    userRstn_pre	:= qdmaInst.io.pcie_arstn & ~controlReg(0)(0)

    withClockAndReset(userClk, false.B) {
        val userRstn_r0    = RegNext(userRstn_pre)
        val userRstn_r1    = RegNext(userRstn_r0)
        val userRstn_r2    = RegNext(userRstn_r1)
        userRstn            := userRstn_r2
    }

    qdmaInst.io.user_clk	:= userClk
    qdmaInst.io.user_arstn	:= qdmaInst.io.pcie_arstn
    qdmaInst.io.soft_rstn	:= 1.U

    qdmaInst.io.pin 		<> qdmaPin

    // Init values
    qdmaInst.io.axib.ar.ready	:= 1.U
    qdmaInst.io.axib.aw.ready	:= 1.U
    qdmaInst.io.axib.w.ready	:= 1.U
    qdmaInst.io.axib.r.valid	:= 1.U
    ToZero(qdmaInst.io.axib.r.bits)
    qdmaInst.io.axib.b.valid	:= 1.U
    ToZero(qdmaInst.io.axib.b.bits)

    // Packet gen module

    val packetGen	= withClockAndReset(userClk, ~userRstn.asBool) {Module(new PacketGenerator())}

    packetGen.io.cmacTx 	<> cmacInst.io.s_net_tx
    packetGen.io.cmacRx		<> cmacInst.io.m_net_rx
    withClockAndReset(userClk, ~userRstn.asBool) {
        packetGen.io.h2cData	<> RegSlice(6)(qdmaInst.io.h2c_data)
        RegSlice(6)(packetGen.io.c2hCmd)		<> qdmaInst.io.c2h_cmd
        RegSlice(6)(packetGen.io.c2hData)	    <> qdmaInst.io.c2h_data
        RegSlice(6)(packetGen.io.h2cCmd)		<> qdmaInst.io.h2c_cmd
        AXIRegSlice(6)(packetGen.io.sAxib)      <> qdmaInst.io.s_axib.get
    }

    /* For worker, QDMA regs are used as below:
     *
     * Reg(128-129): Param phys address
     * Reg(130)    : Param len
     * Reg(132-133): Param req callback
     * Reg(134)    : Param req valid
     * Reg(136-137): Grad phys address
     * Reg(138)    : Grad len
     * Reg(140-141): Grad req callback
     * Reg(142)    : Grad req valid
     * Reg(160)    : Node rank
     * Reg(640)    : Param req ready
     * Reg(641)    : Grad req ready
     */
    packetGen.io.paramReq.bits.addr     := Cat(Seq(controlReg(129), controlReg(128)))
    packetGen.io.paramReq.bits.size     := controlReg(130)
    packetGen.io.paramReq.bits.callback := Cat(Seq(controlReg(133), controlReg(132)))
    packetGen.io.gradReq.bits.addr      := Cat(Seq(controlReg(137), controlReg(136)))
    packetGen.io.gradReq.bits.size      := controlReg(138)
    packetGen.io.gradReq.bits.callback  := Cat(Seq(controlReg(141), controlReg(140)))
    packetGen.io.paramReq.valid         := withClockAndReset(userClk, ~userRstn.asBool){
        (controlReg(134)(0) === 1.U && RegNext(controlReg(134)(0)) =/= 1.U)
    }
    packetGen.io.gradReq.valid          := withClockAndReset(userClk, ~userRstn.asBool){
        (controlReg(142)(0) === 1.U && RegNext(controlReg(142)(0)) =/= 1.U)
    }
    packetGen.io.nodeRank       := controlReg(160)
    packetGen.io.idxTotalLen    := controlReg(161)
    packetGen.io.dataTotalLen   := controlReg(165)
    packetGen.io.engineRand     := controlReg(162)
    packetGen.io.RxIdxDepth     := controlReg(163)
    packetGen.io.IdxTransNum    := controlReg(164)
    packetGen.io.token_small    := controlReg(166)(7,0)
    packetGen.io.token_big      := controlReg(167)(7,0)

    packetGen.io.rxIdxInitAddr   := Cat(Seq(controlReg(151), controlReg(150)))
    packetGen.io.rxDataInitAddr  := Cat(Seq(controlReg(153), controlReg(152)))


    statusReg(128) := packetGen.io.paramReq.ready
    statusReg(129) := packetGen.io.gradReq.ready
    statusReg(130) := packetGen.io.rfinal

    // Tracing performance of FPGA.
    // withClockAndReset(userClk, ~userRstn.asBool) {
    //     val h2cProbe = Module(new BandwidthProbe)
    //     h2cProbe.io.enable 		:= controlReg(208)
    //     h2cProbe.io.fire		:= packetGen.io.h2cData.fire
    //     h2cProbe.io.count.ready	:= (controlReg(209)(0) === 1.U && RegNext(controlReg(209)(0)) =/= 1.U)
    //     statusReg(256)	:= Mux(h2cProbe.io.count.valid, h2cProbe.io.count.bits, -1.S(32.W).asUInt)
    // }

        eng_hbm.io.userClk     	:= userClk
        eng_hbm.io.userRstn    	:= userRstn
        eng_hbm.io.hbmClk      	:= hbmClk
        eng_hbm.io.hbmRstn     	:= hbmRstn	
        eng_hbm.io.hbmCtrlAr   	<> packetGen.io.hbmCtrlAr 
        eng_hbm.io.hbmCtrlR    	<> packetGen.io.hbmCtrlR 
		eng_hbm.io.hbmCtrlAw	<> packetGen.io.hbmCtrlAw 
		eng_hbm.io.hbmCtrlW		<> packetGen.io.hbmCtrlW 

    hbmDriver.io.axi_hbm(8) <>  withClockAndReset(hbmClk,!hbmRstn){AXIRegSlice(1)(eng_hbm.io.hbmAxi)}

    Collector.connect_to_status_reg(statusReg, 300)
}
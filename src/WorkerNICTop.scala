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
import common.partialReconfig.AlveoStaticIO


class WorkerNICTop extends MultiIOModule {
	override val desiredName = "AlveoDynamicTop"
    val io = IO(Flipped(new AlveoStaticIO(
        VIVADO_VERSION = "202101", 
		QDMA_PCIE_WIDTH = 16, 
		QDMA_SLAVE_BRIDGE = true, 
		QDMA_AXI_BRIDGE = true,
		ENABLE_CMAC_1 = false,
		ENABLE_CMAC_2 		= true,
		ENABLE_DDR_1		= false,
		ENABLE_DDR_2		= false,		
    )))


	val dbgBridgeInst = DebugBridge(clk=clock)
	dbgBridgeInst.getTCL()

    dontTouch(io)
	//HBM
	val hbmDriver = withClockAndReset(io.sysClk, false.B) {Module(new HBM_DRIVER(WITH_RAMA=false, IP_CORE_NAME="HBMBlackBox"))}
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



	val qdma = Module(new QDMADynamic(
		VIVADO_VERSION		= "202002",
		PCIE_WIDTH			= 16,
		SLAVE_BRIDGE		= true,
		BRIDGE_BAR_SCALE	= "Megabytes",
		BRIDGE_BAR_SIZE 	= 4
	))



	val controlReg  = qdma.io.reg_control
	val statusReg   = qdma.io.reg_status
	ToZero(statusReg)

	userClk		:= clock
	userRstn	:= ((~reset.asBool & ~controlReg(0)(0)).asClock).asBool

	qdma.io.qdma_port	<> io.qdma
	qdma.io.user_clk	:= userClk
	qdma.io.user_arstn	:= ~reset.asBool


    // Init values
    qdma.io.axib.ar.ready	:= 1.U
    qdma.io.axib.aw.ready	:= 1.U
    qdma.io.axib.w.ready	:= 1.U
    qdma.io.axib.r.valid	:= 1.U
    ToZero(qdma.io.axib.r.bits)
    qdma.io.axib.b.valid	:= 1.U
    ToZero(qdma.io.axib.b.bits)


	// qdma.io.h2c_data.ready	:= 0.U
	qdma.io.c2h_data.valid	:= 0.U
	qdma.io.c2h_data.bits	:= 0.U.asTypeOf(new C2H_DATA)

	// qdma.io.h2c_cmd.valid	:= 0.U
	qdma.io.h2c_cmd.bits	:= 0.U.asTypeOf(new H2C_CMD)
	qdma.io.c2h_cmd.valid	:= 0.U
	qdma.io.c2h_cmd.bits	:= 0.U.asTypeOf(new C2H_CMD)




		val cmacInst2 = Module(new XCMAC(BOARD="u280", PORT=1, IP_CORE_NAME="CMACBlackBox"))
		cmacInst2.getTCL()

		// Connect CMAC's pins
		cmacInst2.io.pin			<> io.cmacPin2.get
		cmacInst2.io.drp_clk		:= io.cmacClk.get
		cmacInst2.io.user_clk		:= userClk
		cmacInst2.io.user_arstn		:= userRstn
		cmacInst2.io.sys_reset		:= reset

		cmacInst2.io.m_net_rx.ready  := 1.U
		ToZero(cmacInst2.io.s_net_tx.bits)
		cmacInst2.io.s_net_tx.valid  := 0.U






    // Packet gen module

    val packetGen	= withClockAndReset(userClk, ~userRstn.asBool) {Module(new PacketGenerator())}

    packetGen.io.cmacTx 	<> cmacInst2.io.s_net_tx
    packetGen.io.cmacRx		<> cmacInst2.io.m_net_rx
    withClockAndReset(userClk, ~userRstn.asBool) {
        packetGen.io.h2cData	<> RegSlice(6)(qdma.io.h2c_data)
        RegSlice(6)(packetGen.io.c2hCmd)		<> qdma.io.c2h_cmd
        RegSlice(6)(packetGen.io.c2hData)	    <> qdma.io.c2h_data
        RegSlice(6)(packetGen.io.h2cCmd)		<> qdma.io.h2c_cmd
        AXIRegSlice(1)(packetGen.io.sAxib)      <> qdma.io.s_axib.get
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
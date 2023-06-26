package p4nic

import chisel3._
import chisel3.util._
import cmac._
import qdma._
import common._
import common.storage._
import common.axi._

// Module name must be AlveoDynamicTop
class AlveoDynamicTop extends MultiIOModule {
    val io = IO(Flipped(new AlveoStaticIO(
        VIVADO_VERSION = "202101", 
		QDMA_PCIE_WIDTH = 16, 
		QDMA_SLAVE_BRIDGE = true, 
		QDMA_AXI_BRIDGE = true
    )))

	// ========================================================
	// For PR debugging. Don't touch!

	// Do not change even a name of the IO definition below.
	val S_BSCAN = IO(new Bundle{
		val drck 		= Input(UInt(1.W))
		val shift 		= Input(UInt(1.W))
		val tdi			= Input(UInt(1.W))
		val update		= Input(UInt(1.W))
		val sel			= Input(UInt(1.W))
		val tdo			= Output(UInt(1.W))
		val tms			= Input(UInt(1.W))
		val tck			= Input(UInt(1.W))
		val runtest		= Input(UInt(1.W))
		val reset		= Input(UInt(1.W))
		val capture		= Input(UInt(1.W))
		val bscanid_en	= Input(UInt(1.W))
	})

	// Debug bridge for PR part
	val dbgBridge	= Module(new DebugBridge())
	dbgBridge.io.clk	:= clock
	dbgBridge.io.S_BSCAN_drck		<> S_BSCAN.drck
	dbgBridge.io.S_BSCAN_shift		<> S_BSCAN.shift
	dbgBridge.io.S_BSCAN_tdi		<> S_BSCAN.tdi
	dbgBridge.io.S_BSCAN_update		<> S_BSCAN.update
	dbgBridge.io.S_BSCAN_sel		<> S_BSCAN.sel
	dbgBridge.io.S_BSCAN_tdo		<> S_BSCAN.tdo
	dbgBridge.io.S_BSCAN_tms		<> S_BSCAN.tms
	dbgBridge.io.S_BSCAN_tck		<> S_BSCAN.tck
	dbgBridge.io.S_BSCAN_runtest	<> S_BSCAN.runtest
	dbgBridge.io.S_BSCAN_reset		<> S_BSCAN.reset
	dbgBridge.io.S_BSCAN_capture	<> S_BSCAN.capture
	dbgBridge.io.S_BSCAN_bscanid_en	<> S_BSCAN.bscanid_en

	// ========================================================

	// Since this is related to partial reonfiguration,
	// All ports, no matter used or not, should be precisely defined.
	// For big model project, 
	dontTouch(io)

	val userClk  	= Wire(Clock())
	val userRstn 	= Wire(Bool())

	val qdmaInst = Module(new QDMADynamic(
		VIVADO_VERSION		= "202101",
		PCIE_WIDTH			= 16,
		TLB_TYPE			= new TLB,
		SLAVE_BRIDGE		= true,
		BRIDGE_BAR_SCALE	= "Megabytes",
		BRIDGE_BAR_SIZE 	= 4
	))

	qdmaInst.io.qdma_port	<> io.qdma
	qdmaInst.io.user_clk	:= userClk
	qdmaInst.io.user_arstn	:= userRstn

	// Connect QDMA's pins
	val controlReg  = qdmaInst.io.reg_control
	val statusReg   = qdmaInst.io.reg_status
	ToZero(statusReg)

	userClk		:= clock
	userRstn	:= ((~reset.asBool & ~controlReg(0)(0)).asClock).asBool

    // In this case AXIB is not used. Just simply init it.
    qdmaInst.io.axib.ar.ready	:= 1.U
    qdmaInst.io.axib.aw.ready	:= 1.U
    qdmaInst.io.axib.w.ready	:= 1.U
    qdmaInst.io.axib.r.valid	:= 1.U
    ToZero(qdmaInst.io.axib.r.bits)
    qdmaInst.io.axib.b.valid	:= 1.U
    ToZero(qdmaInst.io.axib.b.bits)
    ToZero(qdmaInst.io.s_axib.get.aw.bits)
    qdmaInst.io.s_axib.get.aw.valid   := 0.U
    ToZero(qdmaInst.io.s_axib.get.w.bits)
    qdmaInst.io.s_axib.get.w.valid   := 0.U
    ToZero(qdmaInst.io.s_axib.get.ar.bits)
    qdmaInst.io.s_axib.get.ar.valid   := 0.U
    qdmaInst.io.s_axib.get.b.ready	:= 1.U
    qdmaInst.io.s_axib.get.r.ready	:= 1.U

	val cmacInst = Module(new XCMAC(BOARD="u280"))
	cmacInst.getTCL()

	// Connect CMAC's pins 
	cmacInst.io.pin			<> io.cmacPin
	cmacInst.io.drp_clk		:= io.sysClk
	cmacInst.io.user_clk	:= userClk
	cmacInst.io.user_arstn	:= userRstn
	cmacInst.io.sys_reset	:= reset
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

	
    Collector.connect_to_status_reg(statusReg, 400)
}



class DebugBridge extends BlackBox {
	val io = IO(new Bundle{
		val clk = Input(Clock())
		val S_BSCAN_drck = Input(UInt(1.W))
		val S_BSCAN_shift = Input(UInt(1.W))
		val S_BSCAN_tdi = Input(UInt(1.W))
		val S_BSCAN_update = Input(UInt(1.W))
		val S_BSCAN_sel = Input(UInt(1.W))
		val S_BSCAN_tdo = Output(UInt(1.W))
		val S_BSCAN_tms = Input(UInt(1.W))
		val S_BSCAN_tck = Input(UInt(1.W))
		val S_BSCAN_runtest = Input(UInt(1.W))
		val S_BSCAN_reset = Input(UInt(1.W))
		val S_BSCAN_capture = Input(UInt(1.W))
		val S_BSCAN_bscanid_en = Input(UInt(1.W))
	})
}
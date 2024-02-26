package p4nic

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import common.axi._
import common.storage._
import common.connection._
import qdma._
import common._
import cmac._

class PacketRequest extends Bundle {
    val addr     = Output(UInt(64.W))
    val size     = Output(UInt(32.W))
    val callback = Output(UInt(64.W))
}

class PacketGenerator (
    NUM_OF_CON_LAYER    : Int = 4
) extends Module {
    val io = IO(new Bundle {
        val cmacTx	    = Decoupled(new AXIS(512))
        val cmacRx	    = Flipped(Decoupled(new AXIS(512)))

		val c2hCmd		= Decoupled(new C2H_CMD)
		val c2hData	    = Decoupled(new C2H_DATA)
		val h2cCmd		= Decoupled(new H2C_CMD)
		val h2cData	    = Flipped(Decoupled(new H2C_DATA))
        val sAxib       = new AXIB_SLAVE

        val hbmCtrlAw   = (Decoupled(new RoceMsg))
        val hbmCtrlW    = (Decoupled(UInt(512.W)))
        val hbmCtrlAr   = (Decoupled(new RoceMsg))
        val hbmCtrlR    = Flipped(Decoupled(UInt(512.W)))


        val gradReq     = Flipped(Decoupled(new PacketRequest))
        val paramReq    = Flipped(Decoupled(new PacketRequest))
        val rxIdxInitAddr = Input(UInt(64.W))
        val rxDataInitAddr = Input(UInt(64.W))
        val nodeRank    = Input(UInt(48.W))
        val idxTotalLen = Input(UInt(32.W))
        val dataTotalLen= Input(UInt(32.W))
        val engineRand  = Input(UInt(32.W))
        val RxIdxDepth  = Input(UInt(32.W))
        val IdxTransNum = Input(UInt(32.W))
        val rfinal      = Output(Bool())
        //token
        val token_small  = Input(UInt(8.W))
        val token_big    = Input(UInt(8.W))        
    })

    Collector.fire(io.cmacTx)
    Collector.fire(io.cmacRx)
    Collector.fireLast(io.cmacTx)
    Collector.fireLast(io.cmacRx)
    
    // Network Module
    val netRxFifo    = XQueue(new AXIS(512), 1024)

    val data_split = Module(new DataSplitnew())

    val networkInst     = Module(new NetworkStackSpa)

    io.cmacTx           <> networkInst.io.NetTx
    io.cmacRx           <> netRxFifo.io.in
    netRxFifo.io.out           <> networkInst.io.NetRx
    networkInst.io.rxIdxInitAddr    := io.rxIdxInitAddr
    networkInst.io.rxDataInitAddr   := io.rxDataInitAddr
    // networkInst.io.idxTotalLen      := io.idxTotalLen
    // networkInst.io.dataTotalLen     := io.dataTotalLen
    networkInst.io.nodeRank         := io.nodeRank
    networkInst.io.engineRand       := io.engineRand
    networkInst.io.RxIdxDepth       := io.RxIdxDepth
    // networkInst.io.IdxTransNum      := io.IdxTransNum

    networkInst.io.token_small      := io.token_small
    networkInst.io.token_big        := io.token_big
    io.rfinal					    := 	networkInst.io.rfinal
    networkInst.io.reset_final      := io.gradReq.fire()


    // 1. Send parameter request

    val paramReqFifo    = XQueue(new PacketRequest, 1024)
    paramReqFifo.io.in    <> io.paramReq
    

    
    // 2. Push gradient to P4

    val gradReader = Module(new MemoryDataReader)


    Connection.one2many(io.gradReq)(gradReader.io.cpuReq, data_split.io.readReq)
    gradReader.io.cpuReq.bits               := io.gradReq.bits
    data_split.io.readReq.bits              := io.gradReq.bits
    gradReader.io.h2cCmd    <> io.h2cCmd
    gradReader.io.h2cData   <> io.h2cData

    // Now convert gradient data to packets.

    gradReader.io.memData   <> data_split.io.DataIn

    data_split.io.DataOut   <> networkInst.io.DataIn
    data_split.io.IndexOut  <> networkInst.io.IndexIn    
		//hbm interface
    data_split.io.hbmCtrlAw     <> io.hbmCtrlAw
    data_split.io.hbmCtrlW      <> io.hbmCtrlW
    data_split.io.hbmCtrlAr     <> io.hbmCtrlAr
    data_split.io.hbmCtrlR      <> io.hbmCtrlR


    // 4. Pull parameter from P4

    val paramWriter = Module(new MemoryDataWriter)

    val rxCmdRouter    = XArbiter(new PacketRequest, 2)
    rxCmdRouter.io.in(0)   <> paramReqFifo.io.out
    rxCmdRouter.io.in(1)   <> networkInst.io.wrMemCmd


    paramWriter.io.cpuReq   <> rxCmdRouter.io.out
    paramWriter.io.c2hCmd   <> io.c2hCmd
    paramWriter.io.c2hData  <> io.c2hData
    paramWriter.io.memData  <> networkInst.io.DataRx

    // 5. Overall callback handling.

    val callbackWriter = Module(new MemoryCallbackWriter)

    val callbackAbt = XArbiter(UInt(64.W), 2)
    
    callbackAbt.io.in(0) <> paramWriter.io.callback
    callbackAbt.io.in(1) <> gradReader.io.callback
    callbackAbt.io.out   <> callbackWriter.io.callback

    callbackWriter.io.sAxib <> io.sAxib

    

    // Debug module

    val timer_en = RegInit(Bool(), false.B)
    val timer = RegInit(UInt(32.W), 0.U)

    when (io.h2cCmd.fire()) {
        timer_en := true.B
    }.elsewhen(networkInst.io.rfinal){
        timer_en := false.B
    }.otherwise {
        timer_en := timer_en
    }

    when (timer_en) {
        timer := timer + 1.U
    }.otherwise {
        timer := timer
    }

    Collector.report(timer)

    val dbg_cnt = RegInit(UInt(32.W), 0.U)

    when (io.h2cData.ready && ~io.h2cData.valid) {
        dbg_cnt := dbg_cnt + 1.U
    }.otherwise {
        dbg_cnt := 0.U
    }


    // class ila_timer(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlatimer = Module(new ila_timer(Seq(	
    //     timer_en,
    //     timer,
    //     networkInst.io.rfinal
    // )))
    // instIlatimer.connect(clock) 

    dontTouch(timer)
    class ila_debug(seq:Seq[Data]) extends BaseILA(seq)
    val instIlaDbg = Module(new ila_debug(Seq(	
        io.c2hCmd.ready,
        io.c2hCmd.valid,
        // io.c2hCmd.bits.addr,
        // io.c2hCmd.bits.len,
        io.c2hData.ready,
        io.c2hData.valid,
        // io.c2hData.bits.data,
        io.c2hData.bits.last,
        io.h2cCmd.ready,
        io.h2cCmd.valid,
        // io.h2cCmd.bits.addr,
        io.h2cCmd.bits.len,        
        io.h2cData.ready,
        io.h2cData.valid,
        // io.h2cData.bits.data,
        io.h2cData.bits.last, 
        // io.rxIdxInitAddr
        // io.gradReq,
        // io.paramReq,
        // io.NetTx,
        // io.NetRx
    )))
    instIlaDbg.connect(clock)


    val tx_data = Wire(UInt(96.W))
    tx_data := io.cmacTx.bits.data(95,0)
    val rx_data = Wire(UInt(96.W))
    rx_data := io.cmacRx.bits.data(95,0)
    class ila_net(seq:Seq[Data]) extends BaseILA(seq)
    val instIlaNet = Module(new ila_net(Seq(	
        // io.gradReq,
        // io.paramReq,
        io.cmacTx.ready,
        io.cmacTx.valid,
        io.cmacTx.bits.last,
        tx_data,
        io.cmacRx.ready,
        io.cmacRx.valid,
        io.cmacRx.bits.last,
        rx_data
        // netRxFifo.io.out
    )))
    instIlaNet.connect(clock)    

}


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

class DensePacketGen (
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

        val gradReq     = Flipped(Decoupled(new PacketRequest))
        val paramReq    = Flipped(Decoupled(new PacketRequest))
        val nodeRank    = Input(UInt(48.W))
    })

    Collector.fire(io.cmacTx)
    Collector.fire(io.cmacRx)
    Collector.fireLast(io.cmacTx)
    Collector.fireLast(io.cmacRx)
    
    // Network Module
    val netRxFifo    = XQueue(new AXIS(512), 512)

    val dataRxFifo    = XQueue(UInt(512.W), 2048)

    val networkInst     = Module(new NetworkStack)
    networkInst.io.nodeRank := io.nodeRank

    io.cmacTx           <> networkInst.io.NetTx
    io.cmacRx           <> netRxFifo.io.in
    netRxFifo.io.out           <> networkInst.io.NetRx

    // 1. Send parameter request

    val paramReqFifo    = XQueue(new PacketRequest, 1024)
    paramReqFifo.io.in    <> io.paramReq
    

    
    // 2. Push gradient to P4

    val gradReader = Module(new MemoryDataReader)

    gradReader.io.cpuReq    <> io.gradReq
    gradReader.io.h2cCmd    <> io.h2cCmd
    gradReader.io.h2cData   <> io.h2cData

    // Now convert gradient data to packets.

    gradReader.io.memData   <> networkInst.io.DataTx



    // 4. Pull parameter from P4

    val paramWriter = Module(new MemoryDataWriter)

    val rxCmdRouter    = XArbiter(new PacketRequest, 2)
    rxCmdRouter.io.in(0)   <> paramReqFifo.io.out
    rxCmdRouter.io.in(1)   <> networkInst.io.wrMemCmd


    paramWriter.io.cpuReq   <> rxCmdRouter.io.out
    paramWriter.io.c2hCmd   <> io.c2hCmd
    paramWriter.io.c2hData  <> io.c2hData
    paramWriter.io.memData  <> dataRxFifo.io.out
    dataRxFifo.io.in  <> networkInst.io.DataRx


    // 5. Overall callback handling.

    val callbackWriter = Module(new MemoryCallbackWriter)

    val callbackAbt = XArbiter(UInt(64.W), 2)
    
    callbackAbt.io.in(0) <> paramWriter.io.callback
    callbackAbt.io.in(1) <> gradReader.io.callback
    callbackAbt.io.out   <> callbackWriter.io.callback

    callbackWriter.io.sAxib <> io.sAxib

    // Debug module



    val dbg_cnt = RegInit(UInt(32.W), 0.U)

    when (io.h2cData.ready && ~io.h2cData.valid) {
        dbg_cnt := dbg_cnt + 1.U
    }.otherwise {
        dbg_cnt := 0.U
    }

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
        // io.h2cCmd.bits.len,        
        io.h2cData.ready,
        io.h2cData.valid,
        // io.h2cData.bits.data,
        io.h2cData.bits.last,        
        // io.gradReq,
        // io.paramReq,
        // io.NetTx,
        // io.NetRx
    )))
    instIlaDbg.connect(clock)


    // class ila_net(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlaNet = Module(new ila_net(Seq(	
    //     // io.gradReq,
    //     // io.paramReq,
    //     io.cmacTx.ready,
    //     io.cmacTx.valid,
    //     io.cmacTx.bits.last
    //     // netRxFifo.io.out
    // )))
    // instIlaNet.connect(clock)    

}


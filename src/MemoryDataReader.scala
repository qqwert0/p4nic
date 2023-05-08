package p4nic

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._

/* Memory Reader
 * This Module accepts memory read request from CPU via QDMA control reg,
 * reads corresponding data according to memory address and length
 * and finally writes callback signal to memory.
 */
class MemoryDataReader extends Module {
    val io = IO(new Bundle {
        // Request sent from CPU, including address, length and callback address.
        val cpuReq      = Flipped(Decoupled(new PacketRequest))
        // QDMA H2C DMA signals
		val h2cCmd		= Decoupled(new H2C_CMD)
		val h2cData	    = Flipped(Decoupled(new H2C_DATA))
        // Callback signals, which contains callback address to write.
        val callback    = Decoupled(UInt(64.W))
        // Output data stream.
        val memData     = Decoupled(UInt(512.W))
    })

    val reqRecvFifo     = XQueue(new PacketRequest, 4096)
    reqRecvFifo.io.in   <> io.cpuReq
    // Gradients in execution, i.e. reading from GPU.
    val reqExecFifo     = XQueue(new PacketRequest, 512)
    // Gradient data FIFO. 
    val dataFifo        = XQueue(UInt(512.W), 4096, almostfull_threshold = 2048)

    // Used to control read memory commands
    object DataArState extends ChiselEnum {
        val sDataArIdle, sDataArReq = Value
    }
    import DataArState._

    val dataArSt = RegInit(DataArState(), sDataArIdle)

    // Counters tracing gradient buffer read progress
    val reqBaseAddr    = RegInit(UInt(64.W), 0.U)
    val reqDataRemain  = RegInit(UInt(32.W), 0.U)
    val reqFirstBeat   = RegInit(Bool(), false.B)

    switch (dataArSt) {
        is (sDataArIdle) {
            when (dataFifo.io.almostfull.asBool || ~reqExecFifo.io.in.ready) {
                // Gradient FIFO will be full or Callback fifo is full.
                dataArSt := sDataArIdle
            }.otherwise {
                when (reqRecvFifo.io.out.valid || reqFirstBeat) { 
                    dataArSt := sDataArReq
                }.otherwise {
                    dataArSt := sDataArIdle
                }
            }
        }
        is (sDataArReq) {
            when (io.h2cCmd.fire) {
                dataArSt := sDataArIdle
            }.otherwise {
                dataArSt := sDataArReq
            }
        }
    }

    reqRecvFifo.io.out.ready := (dataArSt === sDataArIdle
        && reqDataRemain === 0.U && ~dataFifo.io.almostfull.asBool
        && reqExecFifo.io.in.ready)

    when (reqRecvFifo.io.out.fire) {
        reqBaseAddr     := reqRecvFifo.io.out.bits.addr
        reqDataRemain   := reqRecvFifo.io.out.bits.size
        reqFirstBeat    := true.B
    }.elsewhen (io.h2cCmd.fire) {
        reqDataRemain   := 0.U
        reqFirstBeat    := false.B
    }

    reqExecFifo.io.in.valid := reqRecvFifo.io.out.fire
    reqExecFifo.io.in.bits  := reqRecvFifo.io.out.bits

    val h2cDataCnt = RegInit(UInt(64.W), 0.U)

    when (io.h2cData.fire) {
        when (h2cDataCnt + 64.U(64.W) === reqExecFifo.io.out.bits.size) {
            h2cDataCnt := 0.U(64.W)
        }.otherwise {
            h2cDataCnt := h2cDataCnt + 64.U(64.W)
        }
    }
    reqExecFifo.io.out.ready := (io.h2cData.fire 
        && (h2cDataCnt + 64.U(64.W) === reqExecFifo.io.out.bits.size))

    // Callback write waiting list
    // Notice that theoratically callback may not be successfully
    // to callback FIFO. But here we think this won't happen.
    val callbackFifo = XQueue(UInt(64.W), 16)
    
    callbackFifo.io.in.valid := reqExecFifo.io.out.fire
    callbackFifo.io.in.bits  := reqExecFifo.io.out.bits.callback

    // Send H2C command
    ToZero(io.h2cCmd.bits)
    io.h2cCmd.valid     := (dataArSt === sDataArReq)
    io.h2cCmd.bits.addr := reqBaseAddr
    io.h2cCmd.bits.eop  := 1.U
    io.h2cCmd.bits.sop  := 1.U
    io.h2cCmd.bits.qid  := 0.U
    io.h2cCmd.bits.len  := reqDataRemain

    // H2C data handle.
    dataFifo.io.in.bits     := io.h2cData.bits.data
    // Please note that H2C data MIGHT send some useless zero bits.
    // If that happens, the beat should be thrown away!
    dataFifo.io.in.valid    := io.h2cData.valid & !io.h2cData.bits.tuser_zero_byte
    io.h2cData.ready        := dataFifo.io.in.ready

    dataFifo.io.out         <> io.memData
    callbackFifo.io.out     <> io.callback
}
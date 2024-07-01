package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class XQueueSim extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn   = Flipped(Decoupled(UInt(512.W)))
        // User data
        val DataOut  = (Decoupled(UInt(512.W)))
        //
    })

	Collector.fire(io.DataIn)
	Collector.fire(io.DataOut)

	val count       = RegInit(0.U(32.W))

    io.DataIn.ready := 1.U
	io.DataOut.bits := 1.U
    io.DataOut.valid:= Mux(count > 0.U, 1.U, 0.U)

    when(io.DataIn.fire() & io.DataOut.fire()){
        count       := count
    }.elsewhen(io.DataIn.fire()){
        count       := count + 1.U
    }.elsewhen(io.DataOut.fire()){
        count       := count - 1.U
    }.otherwise{
        count       := count
    }

}

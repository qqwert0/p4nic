package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class NetTxPkg extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetTx      = (Decoupled(new AXIS(512)))
        // User data
        val DataTx      = Flipped(Decoupled(UInt(512.W)))
    })

	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)	


	

	io.DataTx.ready					:= io.NetTx.ready
	ToZero(io.NetTx.valid)
	ToZero(io.NetTx.bits)

	Collector.fire(io.NetTx)
	Collector.fire(io.DataTx)

	switch(state){
		is(sIDLE){
			when(io.DataTx.fire()){
				io.NetTx.valid            := 1.U
                io.NetTx.bits.data        := Util.reverse(io.DataTx.bits)
                io.NetTx.bits.last        := 0.U
				io.NetTx.bits.keep        := -1.S(64.W).asTypeOf(UInt(64.W))
                state                       := sPAYLOAD
			}.otherwise{
				state                       := sIDLE
			}
		}
		is(sPAYLOAD){
			when(io.DataTx.fire()){
				io.NetTx.valid            := 1.U
                io.NetTx.bits.data        := Util.reverse(io.DataTx.bits)
                io.NetTx.bits.last        := 1.U
				io.NetTx.bits.keep        := -1.S(64.W).asTypeOf(UInt(64.W))
                state                       := sIDLE
			}.otherwise{
				state                       := sPAYLOAD
			}
		}
	}


}
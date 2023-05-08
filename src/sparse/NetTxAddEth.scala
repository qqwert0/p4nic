package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class NetTxAddEthSpa extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn   = Flipped(Decoupled(new AXIS(512)))
        val MetaIn   = Flipped(Decoupled(new Meta()))
        // User data
        val DataOut  = (Decoupled(new AXIS(512)))
    })

	val data_fifo = XQueue(new AXIS(512),16)
    val meta_fifo = XQueue(new Meta(),16)
	io.DataIn			<> data_fifo.io.in
    io.MetaIn			<> meta_fifo.io.in

	val sIDLE :: sBLANK :: sHEADER :: sPAYLOAD :: Nil = Enum(4)
	val state                   = RegInit(sIDLE)	
	val meta_reg					= Reg(new Meta())

	Collector.fire(io.DataIn)
	Collector.fire(io.MetaIn)
    Collector.fire(io.DataOut)

	data_fifo.io.out.ready					:= ((state === sHEADER) || (state === sPAYLOAD)) & io.DataOut.ready
    meta_fifo.io.out.ready					:= (state === sIDLE)

	ToZero(io.DataOut.valid)
	ToZero(io.DataOut.bits)




	switch(state){
		is(sIDLE){
			when(meta_fifo.io.out.fire()){
                meta_reg                    := meta_fifo.io.out.bits
                when(meta_fifo.io.out.bits.is_empty){
                    state                       := sBLANK
                }otherwise{
                    state                       := sHEADER
                }
			}
		}
		is(sBLANK){
            io.DataOut.valid            := 1.U
            io.DataOut.bits.data        := Cat(0.U(400.W),meta_reg.head.asUInt)
            io.DataOut.bits.last        := 1.U
			io.DataOut.bits.keep        := Cat(0.U(50.W),-1.S(14.W).asTypeOf(UInt(14.W)))
            state                       := sIDLE
		}        
		is(sHEADER){
			when(data_fifo.io.out.fire()){
                io.DataOut.valid            := 1.U
				io.DataOut.bits             <> data_fifo.io.out.bits
                io.DataOut.bits.data        := Cat(data_fifo.io.out.bits.data(511,112),meta_reg.head.asUInt)
				when(data_fifo.io.out.bits.last === 1.U){
                    state                       := sIDLE
                }.otherwise{
                    state                       := sPAYLOAD
                }
			}
		}
		is(sPAYLOAD){
			when(data_fifo.io.out.fire()){
                io.DataOut.valid            := 1.U
                io.DataOut.bits             <> data_fifo.io.out.bits
				when(data_fifo.io.out.bits.last === 1.U){
                    state                       := sIDLE
                }.otherwise{
                    state                       := sPAYLOAD
                }
			}
		}        
	}

}

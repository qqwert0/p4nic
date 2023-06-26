package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._


class P4SimTx extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn      	= Flipped(Decoupled(new AXIS(512)))
		val MetaIn			= Flipped(Decoupled(new P4Meta()))
        // User data
        val DataOut      	= (Decoupled(new AXIS(512)))
    })

	val meta_fifo = XQueue(new P4Meta(),16)
	val data_fifo = XQueue(new AXIS(512),16)
	io.MetaIn			<> meta_fifo.io.in
	io.DataIn			<> data_fifo.io.in

	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)	
	val eth_head 				= Wire(new ETHHeader())
	val index					= RegInit(0.U(32.W))

	meta_fifo.io.out.ready					:= (state === sIDLE)  & data_fifo.io.out.valid & io.DataOut.ready
	data_fifo.io.out.ready					:= ((state === sIDLE)  & meta_fifo.io.out.valid & io.DataOut.ready) | ((state === sPAYLOAD) & io.DataOut.ready) 

	ToZero(eth_head)
	ToZero(io.DataOut.valid)
	ToZero(io.DataOut.bits)




	switch(state){
		is(sIDLE){
			when(data_fifo.io.out.fire() & meta_fifo.io.out.fire()){
				eth_head.next_idx				:= HToN(meta_fifo.io.out.bits.next_idx)
                eth_head.index            		:= HToN(meta_fifo.io.out.bits.slot_idx)
				eth_head.eth_type            	:= HToN(0x2001.U(16.W))

				io.DataOut.valid            := 1.U
                io.DataOut.bits.data        := Cat(data_fifo.io.out.bits.data(511,144),eth_head.asUInt)
                io.DataOut.bits.last        := data_fifo.io.out.bits.last
				io.DataOut.bits.keep        := data_fifo.io.out.bits.keep			
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
package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class NetTxAddEthSp extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn   = Flipped(Decoupled(new AXIS(512)))
        // User data
        val DataOut  = (Decoupled(new AXIS(512)))
        //
        val next_idx = Input(UInt(32.W))
        val bitmap = Input(UInt(32.W))
        val index = Input(UInt(32.W))
    })

	val data_fifo = XQueue(new AXIS(512),16)
	io.DataIn			<> data_fifo.io.in

	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)	
	val eth_head 				= Wire(new ETHHeader())
	val index					= RegInit(0.U(32.W))

	data_fifo.io.out.ready					:= io.DataOut.ready
	ToZero(eth_head)
	ToZero(io.DataOut.valid)
	ToZero(io.DataOut.bits)




	switch(state){
		is(sIDLE){
			when(data_fifo.io.out.fire()){
                eth_head.next_idx           	:= HToN(io.next_idx)
                eth_head.bitmap            		:= HToN(io.bitmap)
                eth_head.index            		:= HToN(io.index)
				eth_head.eth_type            	:= HToN(0x2001.U(16.W))

				io.DataOut.valid            := 1.U
                io.DataOut.bits.data        := Cat(data_fifo.io.out.bits.data(511,208),eth_head.asUInt)
                io.DataOut.bits.last        := data_fifo.io.out.bits.last
				io.DataOut.bits.keep        := data_fifo.io.out.bits.keep

		          //      index						:= index + 1.U				
				index						:= 0.U				
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

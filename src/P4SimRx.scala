package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._




class P4SimRx extends Module {
    val io = IO(new Bundle {
        val NetRx      	= Flipped(Decoupled(new AXIS(512)))
		val DataOut		= (Decoupled(new AXIS(512)))
		val MetaOUt		= (Decoupled(new P4Meta()))
    })


	val meta_fifo = XQueue(new P4Meta(),16)
	val data_fifo = XQueue(new AXIS(512),16)

	io.MetaOUt			<> meta_fifo.io.out
	io.DataOut			<> data_fifo.io.out	

	val eth_header_tmp = Wire(new ETHHeader())
    ToZero(eth_header_tmp)                  


	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                       = RegInit(sIDLE)	

	
	io.NetRx.ready         := ((state === sIDLE)  & meta_fifo.io.in.ready & data_fifo.io.in.ready) | ((state === sPAYLOAD) & data_fifo.io.in.ready) 

	ToZero(meta_fifo.io.in.valid)
	ToZero(meta_fifo.io.in.bits)
	ToZero(data_fifo.io.in.valid)
	ToZero(data_fifo.io.in.bits)	


	
	switch(state){
		is(sIDLE){
			when(io.NetRx.fire()){
                data_fifo.io.in.valid		:= 1.U
                data_fifo.io.in.bits 		<> io.NetRx.bits
				eth_header_tmp          := io.NetRx.bits.data(207,0).asTypeOf(eth_header_tmp)
                meta_fifo.io.in.valid		:= 1.U
                meta_fifo.io.in.bits.next_idx	:= HToN(eth_header_tmp.next_idx)	
				meta_fifo.io.in.bits.slot_idx	:= HToN(eth_header_tmp.index)				

                when(io.NetRx.bits.last =/= 1.U){
                    state               := sPAYLOAD
					meta_fifo.io.in.bits.has_data	:= true.B
					data_fifo.io.in.valid		:= 1.U
                }.otherwise{
					state               := sIDLE
					meta_fifo.io.in.bits.has_data	:= false.B
					data_fifo.io.in.valid		:= 0.U
				}

			}
		}
		is(sPAYLOAD){
            when(io.NetRx.fire()){
                data_fifo.io.in.bits     <> io.NetRx.bits
                data_fifo.io.in.valid    := 1.U
                when(data_fifo.io.in.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
			

		}		
	}


}
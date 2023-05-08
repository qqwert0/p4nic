package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._


class P4Meta extends Bundle{
	val next_idx 	= UInt(32.W)
	val slot_idx 	= UInt(32.W)
	val has_data 	= Bool()
}

class P4Sim extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetTx      = Vec(2,(Decoupled(new AXIS(512))))
        // Net data
        val NetRx      = Vec(2,Flipped(Decoupled(new AXIS(512))))
    })




	val rx0 = Module(new P4SimRx)
    val rx1 = Module(new P4SimRx)

	val tx 	= Module(new P4SimTx)  

	val eth_lshift0 = Module(new LSHIFT(26,512)) 
	val eth_lshift1 = Module(new LSHIFT(26,512))

	val data_pre0_fifo	= XQueue(new AXIS(512), 8)
	val data_pre1_fifo	= XQueue(new AXIS(512), 8)
	val data_fifo	= XQueue(new AXIS(512), 8)
	val meta_fifo 	= XQueue(new P4Meta(), 8)
	val eth_rshift  = Module(new RSHIFT(26,512))  

	ToZero(data_fifo.io.in.valid)
	ToZero(data_fifo.io.in.bits)
	ToZero(meta_fifo.io.in.valid)
	ToZero(meta_fifo.io.in.bits)	

	io.NetRx(0)	<>rx0.io.NetRx           
	io.NetRx(1)	<>rx1.io.NetRx 

    eth_lshift0.io.in            <>  rx0.io.DataOut
	eth_lshift1.io.in            <>  rx1.io.DataOut
    eth_lshift0.io.out            <>  data_pre0_fifo.io.in
	eth_lshift1.io.out            <>  data_pre1_fifo.io.in

    eth_rshift.io.in   			<>  data_fifo.io.out
	tx.io.DataIn				<>	eth_rshift.io.out
	tx.io.MetaIn				<>	meta_fifo.io.out


	io.NetTx(0).valid		:= tx.io.DataOut.valid
	io.NetTx(0).bits		:= tx.io.DataOut.bits
	io.NetTx(1).valid		:= tx.io.DataOut.valid
	io.NetTx(1).bits		:= tx.io.DataOut.bits

	tx.io.DataOut.ready		:= io.NetTx(0).ready & io.NetTx(1).ready

	val sIDLE :: sALL :: sREAD1 :: sREAD0 :: Nil = Enum(4)
	val state                       = RegInit(sIDLE)	

	
	rx0.io.MetaOUt.ready			:= (state === sIDLE)  & rx1.io.MetaOUt.valid
	rx1.io.MetaOUt.ready			:= (state === sIDLE)  & rx0.io.MetaOUt.valid

	data_pre0_fifo.io.out.ready	:= ((state === sREAD0) & data_fifo.io.in.ready) || ((state === sALL) & data_pre1_fifo.io.out.valid & data_fifo.io.in.ready)
	data_pre1_fifo.io.out.ready	:= ((state === sREAD1) & data_fifo.io.in.ready) || ((state === sALL) & data_pre0_fifo.io.out.valid & data_fifo.io.in.ready)	


	
	switch(state){
		is(sIDLE){
			when(rx0.io.MetaOUt.fire() & rx1.io.MetaOUt.fire()){
				when(rx0.io.MetaOUt.bits.next_idx > rx1.io.MetaOUt.bits.next_idx){
					meta_fifo.io.in.bits.next_idx 	:= rx1.io.MetaOUt.bits.next_idx
					meta_fifo.io.in.bits.slot_idx 	:= rx1.io.MetaOUt.bits.slot_idx
				}.otherwise{
					meta_fifo.io.in.bits.next_idx	:= rx0.io.MetaOUt.bits.next_idx
					meta_fifo.io.in.bits.slot_idx 	:= rx0.io.MetaOUt.bits.slot_idx
				}
				meta_fifo.io.in.valid			:= 1.U
                when(rx0.io.MetaOUt.bits.has_data & rx1.io.MetaOUt.bits.has_data){
					state		:= sALL
				}.elsewhen(!rx0.io.MetaOUt.bits.has_data){
					state		:= sREAD1
				}.otherwise{
					state		:= sREAD0
				}

			}
		}
		is(sALL){
            when(data_pre0_fifo.io.out.fire() & data_pre1_fifo.io.out.fire()){
                data_fifo.io.in.bits     	<> data_pre0_fifo.io.out.bits
				data_fifo.io.in.bits.data	:= data_pre0_fifo.io.out.bits.data + data_pre1_fifo.io.out.bits.data
                data_fifo.io.in.valid    	:= 1.U
                when(data_pre0_fifo.io.out.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
		}
		is(sREAD1){
            when(data_pre1_fifo.io.out.fire()){
                data_fifo.io.in.bits     	<> data_pre1_fifo.io.out.bits
				data_fifo.io.in.bits.data	:= data_pre1_fifo.io.out.bits.data
                data_fifo.io.in.valid    	:= 1.U
                when(data_pre1_fifo.io.out.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
		}
		is(sREAD0){
            when(data_pre0_fifo.io.out.fire()){
                data_fifo.io.in.bits     	<> data_pre0_fifo.io.out.bits
				data_fifo.io.in.bits.data	:= data_pre0_fifo.io.out.bits.data
                data_fifo.io.in.valid    	:= 1.U
                when(data_pre0_fifo.io.out.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
		}
	} 
 


}

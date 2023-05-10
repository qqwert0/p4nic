package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._
import common.connection._



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

    val blank_data_fifo = XQueue(new AXIS(512),16)
    val payload_meta_fifo = XQueue(new Meta(),16)
    val payload_data_fifo = XQueue(new AXIS(512),16)

    val arbiter			= SerialArbiter(new AXIS(512),2)

	val sIDLE :: sBLANK :: sHEADER :: sPAYLOAD :: Nil = Enum(4)
	val state                   = RegInit(sIDLE)	
	val meta_reg					= Reg(new Meta())

	Collector.fire(io.DataIn)
	Collector.fire(io.MetaIn)
    Collector.fire(io.DataOut)

	
    meta_fifo.io.out.ready                  := payload_meta_fifo.io.in.ready & blank_data_fifo.io.in.ready
    data_fifo.io.out.ready					:= ((state === sIDLE) & payload_meta_fifo.io.out.valid || (state === sPAYLOAD)) & payload_data_fifo.io.in.ready
    payload_meta_fifo.io.out.ready			:= (state === sIDLE) & data_fifo.io.out.valid & payload_data_fifo.io.in.ready

	ToZero(blank_data_fifo.io.in.valid)
	ToZero(blank_data_fifo.io.in.bits)
	ToZero(payload_meta_fifo.io.in.valid)
	ToZero(payload_meta_fifo.io.in.bits)
	ToZero(payload_data_fifo.io.in.valid)
	ToZero(payload_data_fifo.io.in.bits)

	when(meta_fifo.io.out.fire()){
        when(meta_fifo.io.out.bits.is_empty){
            blank_data_fifo.io.in.valid            := 1.U
            blank_data_fifo.io.in.bits.data        := Cat(0.U(400.W),meta_fifo.io.out.bits.head.asUInt)
            blank_data_fifo.io.in.bits.last        := 1.U
            blank_data_fifo.io.in.bits.keep        := Cat(0.U(50.W),-1.S(14.W).asTypeOf(UInt(14.W)))
            state                                  := sIDLE
        }otherwise{
            payload_meta_fifo.io.in.valid           := 1.U
            payload_meta_fifo.io.in.bits            := meta_fifo.io.out.bits
            state                                   := sIDLE
        }
	}



	switch(state){
		is(sIDLE){
			when(payload_meta_fifo.io.out.fire() && data_fifo.io.out.fire() ){
                payload_data_fifo.io.in.valid            := 1.U
				payload_data_fifo.io.in.bits             <> data_fifo.io.out.bits
                payload_data_fifo.io.in.bits.data        := Cat(data_fifo.io.out.bits.data(511,112),payload_meta_fifo.io.out.bits.head.asUInt)
				when(data_fifo.io.out.bits.last === 1.U){
                    state                       := sIDLE
                }.otherwise{
                    state                       := sPAYLOAD
                }
			}
		}      
		is(sPAYLOAD){
			when(data_fifo.io.out.fire()){
                payload_data_fifo.io.in.valid            := 1.U
                payload_data_fifo.io.in.bits             <> data_fifo.io.out.bits
				when(data_fifo.io.out.bits.last === 1.U){
                    state                       := sIDLE
                }.otherwise{
                    state                       := sPAYLOAD
                }
			}
		}        
	}

    arbiter.io.in(0)        <>  blank_data_fifo.io.out
    arbiter.io.in(1)        <>  payload_data_fifo.io.out
    arbiter.io.out          <>  io.DataOut

}
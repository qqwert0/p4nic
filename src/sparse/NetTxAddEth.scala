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
        val MetaIn   = Flipped(Decoupled(new Header()))
        // User data
        val DataOut  = (Decoupled(new AXIS(512)))

        val rfinal      = Input(Bool())
    })

	val data_fifo = XQueue(new AXIS(512),4096)
    val meta_fifo = XQueue(new Header(),512)
	io.DataIn			<> data_fifo.io.in
    io.MetaIn			<> meta_fifo.io.in

    val blank_meta_fifo = XQueue(new Header(),16)
    val blank_data_fifo = XQueue(new AXIS(512),16)
    val payload_meta_fifo = XQueue(new Header(),16)
    val payload_data_fifo = XQueue(new AXIS(512),16)
    val reset_data_fifo = XQueue(new AXIS(512),4)

    val arbiter			= SerialArbiter(new AXIS(512),3)

	val sIDLE :: sBLANK :: sPAYLOAD :: sLAST :: Nil = Enum(4)
	val state                   = RegInit(sIDLE)
    val bstate                  	= RegInit(sIDLE)
	val meta_reg					= Reg(new Header())

	Collector.fire(io.DataIn)
	Collector.fire(io.MetaIn)
    Collector.fire(io.DataOut)
    Collector.fire(meta_fifo.io.out)
    Collector.fire(payload_meta_fifo.io.in)
    Collector.fire(payload_meta_fifo.io.out)

	
    meta_fifo.io.out.ready                  := payload_meta_fifo.io.in.ready & blank_meta_fifo.io.in.ready
    data_fifo.io.out.ready					:= (((state === sIDLE) & payload_meta_fifo.io.out.valid) || (state === sPAYLOAD)) & payload_data_fifo.io.in.ready
    blank_meta_fifo.io.out.ready			:= (bstate === sIDLE) & blank_data_fifo.io.in.ready
    payload_meta_fifo.io.out.ready			:= (state === sIDLE) & data_fifo.io.out.valid & payload_data_fifo.io.in.ready


	ToZero(blank_meta_fifo.io.in.valid)
	ToZero(blank_meta_fifo.io.in.bits)
	ToZero(blank_data_fifo.io.in.valid)
	ToZero(blank_data_fifo.io.in.bits)
	ToZero(payload_meta_fifo.io.in.valid)
	ToZero(payload_meta_fifo.io.in.bits)
	ToZero(payload_data_fifo.io.in.valid)
	ToZero(payload_data_fifo.io.in.bits)
	ToZero(reset_data_fifo.io.in.valid)
	ToZero(reset_data_fifo.io.in.bits)


    when(io.rfinal & (!RegNext(io.rfinal))){
        reset_data_fifo.io.in.valid            := 1.U
        reset_data_fifo.io.in.bits.data        := Cat(0.U(400.W),"h0420".U,0.U(96.W))
        reset_data_fifo.io.in.bits.last        := 1.U
        reset_data_fifo.io.in.bits.keep        := -1.S(64.W).asTypeOf(UInt(64.W))        
    }


	when(meta_fifo.io.out.fire()){
        when(meta_fifo.io.out.bits.is_empty){
            blank_meta_fifo.io.in.valid             := 1.U
            blank_meta_fifo.io.in.bits              := meta_fifo.io.out.bits
        }otherwise{
            payload_meta_fifo.io.in.valid           := 1.U
            payload_meta_fifo.io.in.bits            := meta_fifo.io.out.bits
        }
	}


	switch(bstate){
		is(sIDLE){
			when(blank_meta_fifo.io.out.fire()){
                blank_data_fifo.io.in.valid            := 1.U
                blank_data_fifo.io.in.bits.data        := Cat(0.U(400.W),blank_meta_fifo.io.out.bits.head.asUInt)
                blank_data_fifo.io.in.bits.last        := 0.U
                blank_data_fifo.io.in.bits.keep        := -1.S(64.W).asTypeOf(UInt(64.W))
                bstate                                 := sPAYLOAD
			}
		}      
		is(sPAYLOAD){
			when(blank_data_fifo.io.in.ready){
                blank_data_fifo.io.in.valid            := 1.U
                blank_data_fifo.io.in.bits.data        := 0.U
                blank_data_fifo.io.in.bits.last        := 0.U
                blank_data_fifo.io.in.bits.keep        := -1.S(64.W).asTypeOf(UInt(64.W))
                bstate                                  := sLAST
			}
		}
    	is(sLAST){
			when(blank_data_fifo.io.in.ready){
                blank_data_fifo.io.in.valid             := 1.U
                blank_data_fifo.io.in.bits.data         := 0.U
                blank_data_fifo.io.in.bits.last         := 1.U
                blank_data_fifo.io.in.bits.keep         := Cat(0.U(50.W),-1.S(14.W).asTypeOf(UInt(14.W)))
                bstate                                   := sIDLE
			}
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
    arbiter.io.in(2)        <>  reset_data_fifo.io.out
    arbiter.io.out          <>  io.DataOut

    // val blank_data_valid = blank_data_fifo.io.in.valid
    // val blank_data_ready = blank_data_fifo.io.in.ready
    // val paymeta_in_valid = payload_meta_fifo.io.in.valid
    // val paymeta_in_ready = payload_meta_fifo.io.in.ready
    // val paymeta_out_valid = payload_meta_fifo.io.out.valid
    // val paymeta_out_ready = payload_meta_fifo.io.out.ready
    // class ila_txpro(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlatxp = Module(new ila_txpro(Seq(	
    //     meta_fifo.io.out, 
    //     io.DataIn.valid,
    //     io.DataIn.ready,
    //     io.DataIn.bits.last,             
    //     io.DataOut.valid,
    //     io.DataOut.ready,
    //     io.DataOut.bits.last,    
	// 	state
    // )))
    // instIlatxp.connect(clock)  

}
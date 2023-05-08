package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class GenHeader extends Module {
    val io = IO(new Bundle {
        // Net data
        val LocalIndex   = Flipped(Decoupled(UInt(32.W)))
        // User data
        val GlobeIndex  = Flipped(Decoupled(UInt(32.W)))
        //
        val Bitmap      = Input(UInt(32.W))
        val EngineRank  = Input(UInt(32.W))

        val MetaOut  = (Decoupled(new Meta()))
    })

	val local_fifo = XQueue(UInt(32.W),8)
	val globe_fifo = XQueue(UInt(32.W),8)
	io.LocalIndex				<> local_fifo.io.in
	io.GlobeIndex				<> globe_fifo.io.in

	val sIDLE :: sREAD :: sRECV :: Nil = Enum(3)
	val state                   = RegInit(sIDLE)
    val state_reg				= RegInit(0.U(32.W))	
	val index_t					= RegInit(0.U(1.W))
    val index_tmp               = WireInit(0.U(32.W))
    val local_index_tmp         = RegInit(0.U(32.W))
    index_tmp                   := Cat(io.EngineRank(30,0),index_t)

	local_fifo.io.out.ready					:= ((state === sIDLE) || (state === sREAD)) & (io.MetaOut.ready)
    globe_fifo.io.out.ready					:= (state === sRECV) & (io.MetaOut.ready)

	ToZero(io.MetaOut.valid)
	ToZero(io.MetaOut.bits)

    state_reg					:= RegNext(state)
	// Collector.fire(io.LocalIndex)
	// Collector.fire(io.MetaOut)
	// Collector.report(state_reg)


	switch(state){
		is(sIDLE){
			when(local_fifo.io.out.fire()){
                io.MetaOut.bits.head.next_idx           	:= HToN(local_fifo.io.out.bits)
                io.MetaOut.bits.head.bitmap            	    := HToN(io.Bitmap)
                io.MetaOut.bits.head.index            		:= HToN(index_tmp)                
                io.MetaOut.bits.head.eth_type            	:= HToN(0x2001.U(16.W))
                io.MetaOut.bits.is_empty            	    := false.B
                index_t                                     := index_t + 1.U
                io.MetaOut.valid                            := 1.U
                local_index_tmp                 := local_fifo.io.out.bits
                state                           := sRECV			
			}
		}
		is(sREAD){
			when(local_fifo.io.out.fire()){
                io.MetaOut.bits.head.next_idx           	:= HToN(local_fifo.io.out.bits)
                io.MetaOut.bits.head.bitmap            	    := HToN(io.Bitmap)
                io.MetaOut.bits.head.index            		:= HToN(index_tmp)                
                io.MetaOut.bits.head.eth_type            	:= HToN(0x2001.U(16.W))
                io.MetaOut.bits.is_empty            	    := false.B
                index_t                                     := index_t + 1.U
                io.MetaOut.valid                            := 1.U
                local_index_tmp                 := local_fifo.io.out.bits
                state                           := sRECV
			}
		}

		is(sRECV){
			when(globe_fifo.io.out.fire()){
                when(local_index_tmp === globe_fifo.io.out.bits){
                    state                                       := sREAD
                }.otherwise{
                    io.MetaOut.bits.head.next_idx           	:= HToN(local_index_tmp)
                    io.MetaOut.bits.head.bitmap            	    := HToN(io.Bitmap)
                    io.MetaOut.bits.head.index            		:= HToN(index_tmp)                
                    io.MetaOut.bits.head.eth_type            	:= HToN(0x2002.U(16.W))
                    io.MetaOut.bits.is_empty            	    := true.B
                    index_t                                     := index_t + 1.U
                    io.MetaOut.valid                            := 1.U
                    state                                       := sRECV
                }
			}
		}
	}

}

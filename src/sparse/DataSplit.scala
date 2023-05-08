package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class DataSplit extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn   = Flipped(Decoupled(UInt(512.W)))
        // User data
		val IndexOut = (Decoupled(new Idx()))
        val DataOut  = (Decoupled(UInt(512.W)))
        //
        val InxTotalLen = Input(UInt(32.W))
		val DataTotalLen = Input(UInt(32.W))
		val IdxTransNum = Input(UInt(32.W))
    })



	val data_fifo = XQueue(UInt(512.W),16)
	val idx_fifo = XQueue(new Idx(),512)
	io.IndexOut			<> idx_fifo.io.out
	io.DataOut			<> data_fifo.io.out

	val sIDLE :: sIDEX :: sDATA :: sEND :: Nil = Enum(4)
	val state                   = RegInit(sIDLE)
	val state_reg				= RegInit(0.U(32.W))
	val idx_num 				= RegNext(io.InxTotalLen)	//InxTotalLen>>4.U
	val data_num 				= RegNext(io.DataTotalLen) //InxTotalLen<<1.U
	val idx_trans_num			= RegNext(io.IdxTransNum>>4.U)
	val data_trans_num			= RegNext(io.IdxTransNum<<1.U)
	val index_cnt				= RegInit(0.U(32.W))
	val data_cnt				= RegInit(0.U(32.W))
	val index_trans_cnt			= RegInit(0.U(32.W))
	val data_trans_cnt			= RegInit(0.U(32.W))	
	val shift_cnt				= RegInit(0.U(8.W))
    val idx_tmp                 = RegInit(0.U(512.W))
	val engine_idx				= RegInit(0.U(8.W))


	state_reg					:= RegNext(state)
	Collector.fire(io.DataIn)
	Collector.fire(io.IndexOut)
	Collector.fire(io.DataOut)
	Collector.report(state_reg)
	io.DataIn.ready					:= (state === sIDLE) || ((state === sDATA) && (data_fifo.io.in.ready))

	ToZero(idx_fifo.io.in.valid)
	ToZero(idx_fifo.io.in.bits)
	ToZero(data_fifo.io.in.valid)
	ToZero(data_fifo.io.in.bits)



	switch(state){
		is(sIDLE){
			when(io.DataIn.fire()){			
                idx_tmp                     := io.DataIn.bits
				index_cnt					:= index_cnt+1.U	
				index_trans_cnt				:= index_trans_cnt + 1.U			
                state                       := sIDEX
			}
		}
		is(sIDEX){
			when(idx_fifo.io.in.ready){
				when(idx_tmp(31,0) =/= 0.U){
					idx_fifo.io.in.valid            := 1.U
				}.otherwise{
					idx_fifo.io.in.valid            := 0.U
				}
				idx_fifo.io.in.bits.block_idx   := idx_tmp(31,0)
				idx_fifo.io.in.bits.engine_idx   := engine_idx
                idx_tmp                         := Cat(0.U(32.W),idx_tmp(511,32))
                shift_cnt                       := shift_cnt + 1.U
				engine_idx						:= engine_idx + 1.U
				when(engine_idx === (CONFIG.ENG_NUM-1).U){
					engine_idx					:= 0.U
				}
				when((shift_cnt === 15.U) && (index_cnt === idx_num)){
                    shift_cnt                   := 0.U
					index_trans_cnt				:= 0.U
					when(data_cnt === data_num){
						state                   := sEND
					}.otherwise{
						state                   := sDATA
					}
                }.elsewhen((shift_cnt === 15.U) && (index_trans_cnt === idx_trans_num)){
                    shift_cnt                   := 0.U
					index_trans_cnt				:= 0.U
					when(data_cnt === data_num){
						state                   := sIDLE
					}.otherwise{
						state                   := sDATA
					}
                }.elsewhen((shift_cnt === 15.U)){
                    shift_cnt                   := 0.U
                    state                       := sIDLE
                }.otherwise{
                    state                       := sIDEX
                }
			}
		}
		is(sDATA){
			when(io.DataIn.fire()){
                data_fifo.io.in.valid            := 1.U
                data_fifo.io.in.bits             <> io.DataIn.bits
                data_cnt                        := data_cnt + 1.U
				data_trans_cnt					:= data_trans_cnt + 1.U
				when(data_cnt === (data_num - 1.U)){
					data_trans_cnt				:= 0.U
					when(index_cnt === idx_num){
						state                   := sEND
					}.otherwise{
						state                   := sIDLE
					}
                }.elsewhen(data_trans_cnt === (data_trans_num - 1.U)){
					data_trans_cnt				:= 0.U
                    state                       := sIDLE
                }.otherwise{
                    state                       := sDATA
                }
			}
		}	
		is(sEND){
			data_cnt							:= 0.U
			index_cnt							:= 0.U
			state								:= sIDLE
		}		

	}


    // class ila_split(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlasplit = Module(new ila_split(Seq(	
    //     state_reg,
    //     io.IndexOut,
    //     index_cnt,
	// 	data_cnt,
	// 	index_trans_cnt,
	// 	data_trans_cnt,
	// 	shift_cnt,
	// 	engine_idx
    //     // netRxFifo.io.out
    // )))
    // instIlasplit.connect(clock) 

}

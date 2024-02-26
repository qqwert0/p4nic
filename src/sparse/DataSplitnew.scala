package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import common.Delay
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class DataSplitnew extends Module {
    val io = IO(new Bundle {
        // Net data
		val readReq    = Flipped(Decoupled(new PacketRequest))
        val DataIn   = Flipped(Decoupled(UInt(512.W)))

		//hbm interface
        val hbmCtrlAw   = (Decoupled(new RoceMsg))
        val hbmCtrlW    = (Decoupled(UInt(512.W)))
        val hbmCtrlAr   = (Decoupled(new RoceMsg))
        val hbmCtrlR    = Flipped(Decoupled(UInt(512.W)))

        // User data
		val IndexOut = (Decoupled(new Idx()))
        val DataOut  = (Decoupled(UInt(512.W)))

    })

    Collector.fire(io.readReq)
    Collector.fire(io.DataIn)
    Collector.fire(io.hbmCtrlAw)
    Collector.fire(io.hbmCtrlW)
    Collector.fire(io.hbmCtrlAr)
    Collector.fire(io.hbmCtrlR)
    Collector.fire(io.IndexOut)
    Collector.fire(io.DataOut)	

	
    val readReqFifo    = XQueue(new PacketRequest, 1024)
    readReqFifo.io.in    <> io.readReq
    
    val hbmCmdpreFifo    = XQueue(new RoceMsg, 8)

	val hbmCmdFifo    = XQueue(new RoceMsg, 8)

	hbmCmdFifo.io.in    <> Delay(hbmCmdpreFifo.io.out, 100)


	val data_flag	= RegInit(false.B)
	val length		= RegInit(0.U(32.W))
	val indexlength	= RegInit(0.U(32.W))

	val data_fifo = XQueue(UInt(512.W),4096)

	data_fifo.io.out	<> io.DataOut

	val idx_fifo = XQueue(new Idx(),1024)
	io.IndexOut			<> idx_fifo.io.out

	val sIDLE :: sDelay :: sCmd :: sIndexCmd :: sDATA :: sIndex :: Nil = Enum(6)
	val state                   = RegInit(sIDLE)
	val state_index             = RegInit(sIDLE)
	val state_reg				= RegInit(0.U(32.W))	
	val shift_cnt				= RegInit(0.U(8.W))
    val idx_tmp                 = RegInit(0.U(512.W))
	val engine_idx				= RegInit(0.U(8.W))
	val start					= RegInit(false.B)
	val delay_cnt				= RegInit(0.U(8.W))

	state_reg					:= RegNext(state)
	Collector.fire(io.DataIn)
	Collector.fire(io.IndexOut)
	Collector.fire(io.DataOut)
	Collector.report(state_reg)

	readReqFifo.io.out.ready		:= (state === sIDLE)

	io.DataIn.ready					:= ((state === sIndex)&(io.hbmCtrlW.ready)) || ((state === sDATA) & (data_fifo.io.in.ready))
	hbmCmdFifo.io.out.ready			:= ((state_index === sCmd)&(io.hbmCtrlAr.ready))
	io.hbmCtrlR.ready				:= ((state_index === sIndexCmd)&(io.hbmCtrlAr.ready))


	ToZero(hbmCmdpreFifo.io.in.valid)
	ToZero(hbmCmdpreFifo.io.in.bits)
	ToZero(idx_fifo.io.in.valid)
	ToZero(idx_fifo.io.in.bits)
	ToZero(data_fifo.io.in.valid)
	ToZero(data_fifo.io.in.bits)
	ToZero(io.hbmCtrlAw.valid)
	ToZero(io.hbmCtrlAw.bits)
	ToZero(io.hbmCtrlW.valid)
	ToZero(io.hbmCtrlW.bits)
	ToZero(io.hbmCtrlAr.valid)
	ToZero(io.hbmCtrlAr.bits)	

	switch(state){
		is(sIDLE){
			when(readReqFifo.io.out.fire()){	
				length					:= readReqFifo.io.out.bits.size
                when(data_flag){
					state				:= sDATA
				}.otherwise{
					state				:= sIndexCmd
				}
			}
		}
		is(sIndexCmd){
			when(hbmCmdpreFifo.io.in.ready & io.hbmCtrlAw.ready){
				io.hbmCtrlAw.valid				:= 1.U
				io.hbmCtrlAw.bits.length		:= length
				io.hbmCtrlAw.bits.addr			:= "h80000000".U
				hbmCmdpreFifo.io.in.valid			:= 1.U
				hbmCmdpreFifo.io.in.bits.length	:= length
				state							:= sIndex
			}
		}
		is(sIndex){
			when(io.DataIn.fire()){
				io.hbmCtrlW.valid				:= 1.U
				io.hbmCtrlW.bits				:= io.DataIn.bits
				length							:= length - 64.U
				when(length <= 64.U){
					state						:= sIDLE
					data_flag					:= true.B
					start						:= true.B
				}
			}
		}
		is(sDATA){
			when(io.DataIn.fire()){
                data_fifo.io.in.valid            := 1.U
                data_fifo.io.in.bits             <> io.DataIn.bits
				length							:= length - 64.U
				when(length <= 64.U){
					state						:= sIDLE
					data_flag					:= false.B
					start						:= false.B
				}
			}
		}		
	}





	switch(state_index){
		is(sIDLE){
			when(start & (!RegNext(start))){	
				state_index						:= sDelay
			}
		}
		is(sDelay){
			when(delay_cnt === 70.U){	
				delay_cnt						:= 0.U
				state_index						:= sCmd
			}.otherwise{
				delay_cnt						:= delay_cnt + 1.U
			}
		}
		is(sCmd){
			when(hbmCmdFifo.io.out.fire()){	
				indexlength						:= hbmCmdFifo.io.out.bits.length
				io.hbmCtrlAr.valid				:= 1.U
				io.hbmCtrlAr.bits.length		:= hbmCmdFifo.io.out.bits.length
				io.hbmCtrlAr.bits.addr			:= "h80000000".U
				state_index						:= sIndexCmd
			}
		}		
		is(sIndexCmd){
			when(io.hbmCtrlR.fire()){
                idx_tmp                     := io.hbmCtrlR.bits
				indexlength					:= indexlength - 64.U				
                state_index                 := sIndex
			}
		}
		is(sIndex){
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
				when((shift_cnt === 15.U) && (indexlength === 0.U)){
                    shift_cnt                   := 0.U
					state_index                 := sIDLE
                }.elsewhen((shift_cnt === 15.U)){
                    shift_cnt                   := 0.U
                    state_index                 := sIndexCmd
                }.otherwise{
                    state_index                 := sIndex
                }
			}
		}	
		
	}

    class ila_split(seq:Seq[Data]) extends BaseILA(seq)
    val instIlasplit = Module(new ila_split(Seq(	
        io.hbmCtrlW.ready,
		io.hbmCtrlW.valid,
		io.hbmCtrlR.ready,
		io.hbmCtrlR.valid,
		// io.IndexOut
    )))
    instIlasplit.connect(clock) 

}

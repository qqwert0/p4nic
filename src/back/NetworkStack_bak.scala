package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._


class NetworkStackSp extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetTx      = (Decoupled(new AXIS(512)))
        val NetRx      = Flipped(Decoupled(new AXIS(512)))
        // User data
        val DataTx      = Flipped(Decoupled(UInt(512.W)))
        val DataRx      = (Decoupled(UInt(512.W)))
        //write cmd
        val wrMemCmd    = (Decoupled(new PacketRequest))  //addr(64bit),  size(32bit),   callback=0(64bit)

        //config
        val rxIdxInitAddr = Input(UInt(64.W))
        val rxDataInitAddr = Input(UInt(64.W))
        val idxTotalLen = Input(UInt(32.W)) 
        val nodeRank    = Input(UInt(32.W))
        val engineRand  = Input(UInt(32.W)) 

    })
    
    ToZero(io.wrMemCmd.valid)
    ToZero(io.wrMemCmd.bits)

    //net packet:
    //Ether(14B) Index(4B) Bitmap(4B) Next_id(4B) Data(128B) = 26B + 128B = 208bit + 1024bit

    //---------------------------------------------------------------define---------------------------------------
    //para config
    val IDX_TRANS_NUM = 64 //the idx number get from QDMA once
    val RX_IDX_TRANS_NUM = 32 //the idx number put to QDMA once
    val RX_QDMA_LATENCY = 10 //???
    //para calc
    val IDX_DEPTH = IDX_TRANS_NUM/16 //calc the idx depth
    val RX_IDX_TRANS_CYCLE = RX_IDX_TRANS_NUM/16
    val RX_DATA_TRANS_CYCLE = RX_IDX_TRANS_NUM/16*32
    val RX_TOTAL_TRANS_CYCLE = RX_IDX_TRANS_CYCLE + RX_DATA_TRANS_CYCLE
    val RX_IDX_DEPTH = RX_IDX_TRANS_CYCLE + RX_QDMA_LATENCY //calc the idx depth
    val RX_DATA_DEPTH = RX_DATA_TRANS_CYCLE + RX_IDX_DEPTH*2 // *2 for safe,min is RX_IDX_DEPTH if idx is read out continuously
    //-------------------------------------------signal
    //TX
    val cmd_idx_init_addr = WireInit(0.U(64.W)) //!!!tmp
    val cmd_data_init_addr = WireInit(0.U(64.W)) //!!!tmp 
    cmd_idx_init_addr   := io.rxIdxInitAddr
    cmd_data_init_addr  := io.rxDataInitAddr
    val idx_in_total_num = Wire(UInt(32.W)) //!!!tmp total idx num from QDMA,should get from input
    val sIdxIDLE :: sIdxREAD :: Nil = Enum(2)
    val idx_state  = RegInit(sIdxIDLE)	
    val idx_fifo_out_ready = RegInit(0.U(1.W))
    val din_cnt = RegInit(0.U(13.W))
    val din_sel = RegInit(0.U(1.W))
    val start = RegInit(0.U(1.W))
    //val idx_depth = WireDefault(128.U(8.W)) //depth: 8192byte/512bit= 128
    val idx_depth = WireDefault(IDX_DEPTH.U(8.W)) //depth: 8192byte/512bit= 128
    val din_num = Wire(UInt(13.W))
    //val idx_din = RegInit(0.U(512.W))
    //val idx_din_valid = RegInit(0.U(1.W))
    //val data_din = RegInit(0.U(512.W))
    //val data_din_valid = RegInit(0.U(1.W))
    val idx_din = Wire(UInt(512.W))
    val idx_din_valid = Wire(UInt(1.W))
    val data_din = Wire(UInt(512.W))
    val data_din_valid = Wire(UInt(1.W))
    val idx_r = RegInit(0.U(512.W))
    val local_idx = Wire(UInt(32.W))
    val local_idx_t = Wire(UInt(32.W))
    val global_idx = RegInit(0.U(32.W))
    val global_idx_wire = WireInit(0.U(32.W))
    val global_idx_valid = RegInit(0.U(1.W))
    val global_idx_valid_fake = RegInit(0.U(1.W))
    val global_idx_valid_d1 = RegInit(0.U(1.W))
    val shift_cnt = RegInit(0.U(4.W))
    val idx_in_last = Wire(UInt(1.W))
    val din_last = Wire(UInt(1.W))
   // val idx_rd_en = RegInit(0.U(1.W))
    val idx_rd_en = Wire(UInt(1.W))
    val idx_equal_flg = Wire(UInt(1.W))
    val idx_shift_en= Wire(UInt(1.W))
    val idx_fifo_rd_en = RegInit(0.U(1.W))
    val idx_rd_cnt = RegInit(0.U(4.W))
    val idx_rd_total_cnt = RegInit(0.U(32.W))//width tmp
    val idx_rd_finish = RegInit(0.U(1.W))
    val net_din_cnt = RegInit(0.U(2.W))
    val first_trans_flg = RegInit(0.U(1.W))
    val sIDLE :: sFIRST :: sWAIT_G :: sWAIT_L :: sNEXT :: sBLANK :: Nil = Enum(6)  
    val state = RegInit(sIDLE)	
    val tx_add_eth_dout_ready = RegInit(0.U(1.W))
    val global_finish= Wire(UInt(1.W))
    val idx_end_sel = RegInit(0.U(1.W))
    val blank_eth_head  = Wire(new ETHHeader())
    val index = RegInit(0.U(1.W))
    val index_t = Wire(UInt(32.W))
    //RX
    val rx_shift_cnt = RegInit(0.U(4.W))
    val rx_idx_r = RegInit(0.U(512.W))
    val rx_idx_fifo_wren = RegInit(0.U(1.W))
    val rx_idx_cnt = RegInit(0.U(13.W))
    val dout_cnt = RegInit(0.U(13.W))
    val dout_sel = RegInit(0.U(1.W))
    val dout_trans_finish = RegInit(0.U(1.W))
    val cmd_net_din_cnt = RegInit(0.U(32.W))
    val cmd_net_din_cnt_byte = Wire(UInt(32.W))
    val cmd_net_din_cnt_ext = Wire(UInt(32.W))
    val cmd_addr = RegInit(0.U(64.W))
    val cmd_idx_next_addr = RegInit(0.U(64.W))
    val cmd_data_next_addr = RegInit(0.U(64.W))
    val cmd_size = RegInit(0.U(32.W))
    val cmd_data_size = RegInit(0.U(32.W))
    val cmd_valid = RegInit(0.U(1.W))
    val cmd_valid_d1 = RegInit(0.U(1.W))
	val dout_trans_last = RegInit(0.U(1.W))
    val dout_idx_last = Wire(UInt(1.W))
    //val rx_idx_trans_cycle = RegInit(0.U(13.W))
    //val rx_data_trans_cycle = RegInit(0.U(13.W))
    //val rx_total_trans_cycle = RegInit(0.U(13.W))
    //val sIDX :: sDATA :: Nil = Enum(2)  
    //val rx_state = RegInit(sIDX)	
    //-------------------------------------------module
    //TX
    val idx_fifo = XQueue(UInt(512.W),IDX_DEPTH)  
    val data_fifo = XQueue(UInt(512.W),8)  
    val tx_pkg_gen = Module(new NetTxPkg())
    val eth_lshift = Module(new LSHIFT(26,512)) 
    val tx_add_eth = Module(new NetTxAddEthSp())
    //RX
    val rx_idx_fifo = XQueue(UInt(512.W),RX_IDX_DEPTH)  
    val rx_data_fifo = XQueue(UInt(512.W),RX_DATA_DEPTH)  
    val cmd_fifo = XQueue(UInt(160.W),8)  
    val eth_rshift = Module(new RSHIFT(26,512))
    //-----------------------------------------------------------------code---------------------------------------
    //always ready to receive idx,data ready is ctrled
    //io.DataTx.ready := 1.U
    when(din_sel === 0.U){
       // io.DataTx.ready := 1.U  
        io.DataTx.ready := idx_fifo.io.in.ready
    }.otherwise{
        //io.DataTx.ready := tx_pkg_gen.io.DataTx.ready
        io.DataTx.ready := data_fifo.io.in.ready
    }

    //---------------------------------------------------switch idx or data
    //packet din number = idx+data(idx*32)
    //cnt to idx number,tmp:128
    //din_num := Cat(idx_depth,0.U(5.W)) + idx_depth
    din_num := Cat(idx_depth,0.U(5.W)) + idx_depth 
    
    when(io.DataTx.fire() && din_cnt === (din_num-1.U)){
        din_cnt := 0.U
    }.elsewhen(io.DataTx.fire()){
        din_cnt := din_cnt + 1.U
    }

    //
    idx_in_last :=  din_cnt===(idx_depth-1.U) && io.DataTx.fire()
    din_last := din_cnt === (din_num-1.U) && io.DataTx.fire()

    when(idx_in_last===1.U && io.DataTx.fire()){ 
        din_sel := 1.U
    }.elsewhen(io.DataTx.fire() && din_last===1.U){
        din_sel := 0.U
    }

    when(din_sel === 0.U){
        idx_din := io.DataTx.bits
        idx_din_valid := io.DataTx.valid
    }.otherwise{
        idx_din := 0.U
        idx_din_valid := 0.U

    }

    when(din_sel === 1.U){
        data_din := io.DataTx.bits
        data_din_valid := io.DataTx.valid
    }.otherwise{
        data_din := 0.U
        data_din_valid := 0.U
    }

    //!!!!!!!finish
    when(din_last===1.U){
        first_trans_flg := 1.U
    }.elsewhen(global_finish===1.U){
        first_trans_flg := 0.U
    }

    //---------------------------------------------------------idx
    //--------------------idx fifo 
    
    idx_fifo.io.in.bits := idx_din
    idx_fifo.io.in.valid := idx_din_valid


    //-------------------idx fifo ctrl
    idx_fifo.io.out.ready := (idx_in_last===1.U && first_trans_flg===0.U) || idx_fifo_rd_en===1.U //first trans is different

    //------------------shift idx 4B
    when(idx_rd_en === 1.U){
      when(idx_rd_cnt =/= 15.U){ 
        idx_rd_cnt := idx_rd_cnt + 1.U
      }.otherwise{
        idx_rd_cnt := 0.U
      }
    }
	
	idx_in_total_num := io.idxTotalLen;

    when(idx_rd_en === 1.U){
      when(idx_rd_total_cnt =/= (idx_in_total_num - 3.U)){ //discard the last idx
        idx_rd_total_cnt := idx_rd_total_cnt + 1.U
      }.otherwise{
        idx_rd_total_cnt := 0.U
      }
    }

    when((idx_rd_total_cnt === (idx_in_total_num - 3.U)) && idx_rd_en === 1.U){ //discard the last idx 
        idx_rd_finish := 1.U
    }.elsewhen(state === sIDLE){
        idx_rd_finish := 0.U
    }

    when(idx_rd_finish === 1.U && idx_equal_flg === 1.U){
        idx_end_sel := 1.U
    }.elsewhen(state === sIDLE){
        idx_end_sel := 0.U
    }

    when(global_idx_valid === 1.U && global_idx === local_idx){
        idx_equal_flg := 1.U
    }.otherwise{
        idx_equal_flg := 0.U
    }
    
    when(global_idx_valid === 1.U && global_idx ==="hffffffff".U){
        global_finish := 1.U
    }.otherwise{
        global_finish := 0.U
    }

    idx_rd_en := idx_equal_flg ===1.U && idx_rd_finish === 0.U
    idx_shift_en := idx_rd_en === 1.U && idx_rd_cnt =/= 15.U

    //start another trans
    when(idx_rd_en === 1.U && idx_rd_cnt === 15.U){ 
        idx_fifo_rd_en := 1.U
    }.elsewhen(idx_fifo.io.out.fire()){
        idx_fifo_rd_en := 0.U
    }
      
    when(idx_fifo.io.out.fire()){
        idx_r := idx_fifo.io.out.bits
    }.elsewhen(idx_shift_en === 1.U){
        idx_r := Cat(0.U(32.W),idx_r(511,32))
    }

    //local_idx := HToN(idx_r(31,0))
    local_idx := idx_r(31,0)

    when(idx_end_sel === 0.U){
        local_idx_t := local_idx
    }.otherwise{
        local_idx_t := "hffffffff".U
    }
    //----------------------------------------------------------index
	when(state===sIDLE){
		index := 0.U	
	}.elsewhen(global_idx_valid===1.U){
		index := index + 1.U	
    }

	index_t := Cat(io.engineRand(30,0),index)

    // class ila_logic(seq:Seq[Data]) extends BaseILA(seq)
    // val instlogic = Module(new ila_logic(Seq(	
    //     index,
    //     global_idx_valid,
    //     tx_add_eth.io.DataIn.ready,
    //     tx_add_eth.io.DataIn.valid,
    //     tx_add_eth.io.DataIn.bits.last,
    //     // netRxFifo.io.out
    // )))
    // instlogic.connect(clock) 

    //----------------------------------------------------------NET TX
    //--------------------data fifo,use depth=1 reg fifo for better timing
    data_fifo.io.in.valid := data_din_valid
    data_fifo.io.in.bits := data_din
    data_fifo.io.out.ready := tx_pkg_gen.io.DataTx.ready
    //--------------------pkg gen
    //tx_pkg_gen.io.DataTx.valid := data_din_valid
    tx_pkg_gen.io.DataTx.valid := data_fifo.io.out.valid
    tx_pkg_gen.io.DataTx.bits := data_fifo.io.out.bits
    //--------------------shift data
    eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
    //--------------------tx add eth
    tx_add_eth.io.DataIn        <>  eth_lshift.io.out
    tx_add_eth.io.next_idx      :=  local_idx_t
    tx_add_eth.io.bitmap        :=  io.nodeRank 
    tx_add_eth.io.index         :=  index_t
    //
    when(tx_add_eth.io.DataOut.bits.last === 1.U){
        tx_add_eth_dout_ready := 0.U
    }.elsewhen(state === sNEXT || state === sFIRST){
        tx_add_eth_dout_ready := 1.U
    }

    tx_add_eth.io.DataOut.ready := tx_add_eth_dout_ready 
    //-------------------out
    blank_eth_head.next_idx            	:= HToN(local_idx)
    blank_eth_head.bitmap            	:= HToN(io.nodeRank)
    blank_eth_head.index            	:= HToN(index_t)
    blank_eth_head.eth_type            	:= HToN(0x2002.U(16.W))

    when(state === sBLANK){
        io.NetTx.bits.last := 1.U
        io.NetTx.bits.data := Cat(0.U(304.W),blank_eth_head.asUInt)
        io.NetTx.bits.keep := Cat(0.U(38.W),-1.S(26.W).asTypeOf(UInt(26.W)))//26
        io.NetTx.valid := 1.U
    }.otherwise{
        io.NetTx.bits.last := tx_add_eth.io.DataOut.bits.last
        io.NetTx.bits.data := tx_add_eth.io.DataOut.bits.data
        io.NetTx.bits.keep := tx_add_eth.io.DataOut.bits.keep
        io.NetTx.valid := tx_add_eth.io.DataOut.valid

    }
    //-------------------------------------------module
    //--------------------------------------------------------FSM
    switch(state){
        is(sIDLE){
            when(first_trans_flg === 0.U && data_din_valid === 1.U){
                state := sFIRST
            }.otherwise{
                state := sIDLE
            }
        }
        is(sFIRST){
            //when(tx_add_eth.io.DataOut.fire()){
            when(tx_add_eth.io.DataOut.bits.last === 1.U){
                state := sWAIT_G
            }.otherwise{
                state := sFIRST
            }
        }
        is(sWAIT_G){
            when(global_finish === 1.U){
                state := sIDLE
            }.elsewhen(idx_rd_en === 1.U && idx_rd_cnt === 15.U){  //when idx_rd_cnt=15,need to wait until idx fifo dout
                state := sWAIT_L
            }.elsewhen(idx_equal_flg === 1.U){//if shift idx,do not need wait
                state := sNEXT
            }.elsewhen(global_idx_valid === 1.U && global_idx =/= local_idx){
                state := sBLANK
            }.otherwise{
                state := sWAIT_G
            }
        }
        is(sWAIT_L){
            when(idx_fifo.io.out.fire()){ 
                state := sNEXT
            }.otherwise{
                state := sWAIT_L
            }
        }
        is(sNEXT){
            when(tx_add_eth.io.DataOut.bits.last === 1.U){
                state := sWAIT_G
            }.otherwise{
                state := sNEXT
            }
        }
        is(sBLANK){
            state := sWAIT_G
        }
    }


    //----------------------------------------------------------NET RX
	//start for clear
	when(start === 1.U){
		start := 0.U	
	}.elsewhen(first_trans_flg === 0.U && idx_in_last === 1.U){
		start := 1.U 
	}

    io.NetRx.ready := 1.U
    //din cnt
   // when(io.NetRx.fire() && net_din_cnt===2.U){
    when(io.NetRx.bits.last === 1.U){
        net_din_cnt := 0.U
    }.elsewhen(io.NetRx.fire()){
        net_din_cnt := net_din_cnt + 1.U
    }
    //get net rx idx
    global_idx_wire := io.NetRx.bits.data(208,176)
    when(io.NetRx.fire() && net_din_cnt === 0.U){
        //global_idx := HToN(io.NetRx.bits.data(208,176))
        global_idx := HToN(global_idx_wire)
    }

    when(io.NetRx.fire() && net_din_cnt === 0.U){
        global_idx_valid := 1.U
    }.otherwise{
        global_idx_valid := 0.U
    }
  	
	//global idx valid fake:for last trans
	when(global_idx_valid_fake === 1.U && rx_shift_cnt === 15.U){
		global_idx_valid_fake := 0.U
	}.elsewhen(global_finish === 1.U && rx_shift_cnt =/= 15.U){
		global_idx_valid_fake := 1.U
	}
	
    //global idx shift in
    when(global_idx_valid ===1.U){
        rx_idx_r := Cat(global_idx,rx_idx_r(511,32))
    }.elsewhen(global_idx_valid_fake === 1.U){
        rx_idx_r := Cat(0.U(32.W),rx_idx_r(511,32))
	}

    when(global_idx_valid === 1.U || global_idx_valid_fake === 1.U){
      when(rx_shift_cnt =/= 15.U){ 
        rx_shift_cnt := rx_shift_cnt + 1.U
      }.otherwise{
        rx_shift_cnt := 0.U
      }
    }
    
    when(rx_idx_fifo_wren === 1.U){
        rx_idx_fifo_wren := 0.U
	}.elsewhen((global_idx_valid === 1.U || global_idx_valid_fake === 1.U) && rx_shift_cnt === 15.U){
        rx_idx_fifo_wren := 1.U
    }
    
    //push idx to fifo
    rx_idx_fifo.io.in.valid := rx_idx_fifo_wren
    rx_idx_fifo.io.in.bits := rx_idx_r

    //get net rx data
    eth_rshift.io.in            <>  io.NetRx

    //push data to fifo
    rx_data_fifo.io.in.bits     :=  eth_rshift.io.out.bits.data
    rx_data_fifo.io.in.valid    :=  eth_rshift.io.out.valid
    eth_rshift.io.out.ready     :=  rx_data_fifo.io.in.ready

    //send QDMA write cmd

	//size = index_byte(cmd_net_din_cnt*4) + data_byte(cmd_net_din_cnt*4*32)
    when(start === 1.U || (global_idx_valid===1.U && cmd_net_din_cnt===(RX_IDX_TRANS_NUM.asUInt-1.U))){
        cmd_net_din_cnt := 0.U
    }.elsewhen(global_idx_valid===1.U){
        cmd_net_din_cnt := cmd_net_din_cnt + 1.U
    }

	cmd_net_din_cnt_byte :=	Cat((cmd_net_din_cnt+1.U),0.U(2.W))
	cmd_net_din_cnt_ext := cmd_net_din_cnt_byte(31,6) + "b1000000".U //for last trans idx num,64 byte

	when(start === 1.U){
		cmd_size := 0.U
		cmd_data_size := cmd_data_init_addr
		cmd_addr := cmd_idx_init_addr
		cmd_idx_next_addr := cmd_idx_init_addr
		cmd_data_next_addr := cmd_data_init_addr
	}.elsewhen(global_idx_valid===1.U && cmd_net_din_cnt===(RX_IDX_TRANS_NUM.asUInt-1.U)){
		cmd_size := Cat((cmd_net_din_cnt+1.U),0.U(2.W)) //idx
		cmd_data_size := Cat((cmd_net_din_cnt+1.U),0.U(7.W)) //data
		cmd_addr := cmd_idx_next_addr
		cmd_idx_next_addr := cmd_idx_next_addr + Cat((cmd_net_din_cnt+1.U),0.U(2.W))
	}.elsewhen(global_finish===1.U){
		cmd_size := cmd_net_din_cnt_ext //idx
		cmd_data_size := Cat((cmd_net_din_cnt+1.U),0.U(7.W)) //data
		cmd_addr := cmd_idx_next_addr
		cmd_idx_next_addr := 0.U
	}.elsewhen(cmd_valid === 1.U){  //data cmd
		cmd_size := cmd_data_size
		cmd_addr := cmd_data_next_addr
		cmd_data_next_addr := cmd_data_next_addr + cmd_data_size
	}

	when(cmd_valid === 1.U){
		cmd_valid := 0.U
	}.elsewhen((global_idx_valid===1.U && cmd_net_din_cnt===(RX_IDX_TRANS_NUM.asUInt-1.U)) || (global_finish===1.U)){
		cmd_valid := 1.U
	}
	
	cmd_valid_d1 := cmd_valid	
	
	//cmd fifo	
	cmd_fifo.io.in.valid := cmd_valid | cmd_valid_d1
	cmd_fifo.io.in.bits := Cat(cmd_addr,cmd_size,0.U(64.W))

    io.wrMemCmd.valid := cmd_fifo.io.out.valid
    io.wrMemCmd.bits.addr := cmd_fifo.io.out.bits(159,96)
    io.wrMemCmd.bits.size := cmd_fifo.io.out.bits(95,64)
    cmd_fifo.io.out.ready := io.wrMemCmd.ready

    //dout sel
    //last idx,must have 32'hffffffff	
	dout_idx_last := (io.DataRx.fire() && (io.DataRx.bits(31,0).andR || 
										   io.DataRx.bits(63,32).andR ||
										   io.DataRx.bits(95,64).andR ||
										   io.DataRx.bits(127,96).andR ||
										   io.DataRx.bits(159,128).andR ||
										   io.DataRx.bits(191,160).andR ||
										   io.DataRx.bits(223,192).andR ||
										   io.DataRx.bits(255,224).andR ||
										   io.DataRx.bits(287,256).andR ||
										   io.DataRx.bits(319,288).andR ||
										   io.DataRx.bits(351,320).andR ||
										   io.DataRx.bits(383,352).andR ||
										   io.DataRx.bits(415,384).andR ||
										   io.DataRx.bits(447,416).andR ||
										   io.DataRx.bits(479,448).andR ||
										   io.DataRx.bits(511,480).andR))


    when(start === 1.U || (io.DataRx.fire() && dout_cnt===(RX_TOTAL_TRANS_CYCLE.asUInt-1.U))){
        dout_cnt := 0.U
    }.elsewhen(io.DataRx.fire()){
        dout_cnt := dout_cnt + 1.U    
    }

    when(start===1.U || (io.DataRx.fire() && dout_cnt===(RX_TOTAL_TRANS_CYCLE.asUInt-1.U))){
        dout_sel := 0.U
    }.elsewhen((io.DataRx.fire() && dout_cnt===(RX_IDX_TRANS_CYCLE.asUInt-1.U)) || dout_idx_last===1.U){
        dout_sel := 1.U    
    }

    when(dout_sel===1.U){
        io.DataRx.valid := rx_data_fifo.io.out.valid
        io.DataRx.bits := rx_data_fifo.io.out.bits
        rx_data_fifo.io.out.ready := io.DataRx.ready
        rx_idx_fifo.io.out.ready := 0.U
    }.otherwise{
        io.DataRx.valid := rx_idx_fifo.io.out.valid
        io.DataRx.bits := rx_idx_fifo.io.out.bits
        rx_idx_fifo.io.out.ready := io.DataRx.ready
        rx_data_fifo.io.out.ready := 0.U
    }

    //dout trans finish
//	when(dout_trans_finish === 1.U){
//		dout_trans_last := 0.U
//	}
//	.elsewhen(global_finish === 1.U){
//		dout_trans_last := 1.U
//	}
//
//	when(dout_trans_finish===1.U){
//		dout_trans_finish := 0.U
//	}.elsewhen(dout_trans_last===1.U && rx_data_fifo.empty === 1.U){
//		dout_trans_finish := 1.U
//	}
    //--------------------------------debug	
    val debug_cnt = RegInit(0.U(32.W))
    val debug_cnt_1 = RegInit(0.U(32.W))

	when(io.DataTx.fire()){
		debug_cnt := debug_cnt + 1.U	
	}

	when(io.DataRx.fire()){
		debug_cnt_1 := debug_cnt_1 + 1.U	
	}
		


 /*   //TX
    tx_pkg_gen.io.DataTx        <>  io.DataTx
    eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
    sparse.io.DataIn        <>  eth_lshift.io.out
    io.NetTx                    <>  tx_add_eth.io.DataOut

    //RX
    eth_rshift.io.in            <>  io.NetRx
    io.DataRx.bits              :=  eth_rshift.io.out.bits.data
    io.DataRx.valid             :=  eth_rshift.io.out.valid
    eth_rshift.io.out.ready     :=  io.DataRx.ready
*/

     //----------------tmp
 //   io.NetTx.bits.keep := 0.U
 //   io.NetTx.valid := 0.U
 //   io.DataRx.bits := 0.U
//    io.NetTx.bits.last := 0.U
//    io.NetTx.bits.data :=0.U
 //   io.DataRx.valid := 0.U
    dontTouch(din_cnt)
    dontTouch(idx_din)
    dontTouch(idx_din_valid)
    dontTouch(data_din)
    dontTouch(data_din_valid)
    dontTouch(idx_fifo.io.out.valid)
    dontTouch(idx_fifo.io.out.bits)
    dontTouch(io.NetRx)
    dontTouch(io.NetTx)
    dontTouch(io.DataRx)
    dontTouch(idx_equal_flg)
    dontTouch(idx_rd_en)
    dontTouch(local_idx)
    dontTouch(local_idx_t)
    dontTouch(dout_cnt)
    dontTouch(dout_sel)
    dontTouch(debug_cnt)
    dontTouch(debug_cnt_1)


}




//    switch(rx_state){
//        is(sIDX){
//			when(){
//				rx_state := sDATA
//			}.otherwise{
//				rx_state := sIDX
//			}
//        }
//        is(sDATA){
//			when(){
//				rx_state := sIDX
//			}.otherwise{
//				rx_state := sDATA
//		}
//	}

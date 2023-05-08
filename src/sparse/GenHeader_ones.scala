// package p4nic

// import chisel3._
// import chisel3.util._
// import common.axi._
// import chisel3.experimental.ChiselEnum
// import common.storage._
// import qdma._
// import common._



// class GenHeader extends Module {
//     val io = IO(new Bundle {
//         // Net data
//         val LocalIndex   = Flipped(Decoupled(new Idx()))
//         // User data
//         val GlobalIndex  = Flipped(Decoupled(new Idx()))
//         //
//         val Bitmap      = Input(UInt(32.W))
//         // val EngineRank  = Input(UInt(32.W))

//         val MetaOut  = (Decoupled(new Meta()))
//     })

// 	val local_fifo = XQueue(new Idx(),8)
// 	val global_fifo = XQueue(new Idx(),8)
// 	io.LocalIndex				<> local_fifo.io.in
// 	io.GlobalIndex				<> global_fifo.io.in

// 	val index_table = XRam(new INDEX_STATE(), CONFIG.ENGINE_NUM, latency = 1)

// 	val sIDLE :: sREAD :: sRECV :: Nil = Enum(3)
// 	val state                   = RegInit(sIDLE)
//     val local_idx = RegInit(0.U.asTypeOf(new Idx()))
//     val global_idx = RegInit(0.U.asTypeOf(new Idx()))


//     val index_tmp               = WireInit(0.U(32.W))
//     val gindex_tmp              = WireInit(0.U(32.W))
//     val local_index_tmp         = RegInit(0.U(32.W))
//     index_tmp                   := Cat(local_idx.engine_idx,index_table.io.data_out_b.slot_idx)
//     gindex_tmp                  := Cat(global_idx.engine_idx,index_table.io.data_out_b.slot_idx)

// 	local_fifo.io.out.ready					    :=  (io.MetaOut.ready)
//     global_fifo.io.out.ready					:= (!local_fifo.io.out.valid) & (io.MetaOut.ready)

// 	ToZero(io.MetaOut.valid)
// 	ToZero(io.MetaOut.bits)

//     ToZero(index_table.io.addr_a)                 
//     ToZero(index_table.io.addr_b)                 
//     ToZero(index_table.io.wr_en_a)                
//     ToZero(index_table.io.data_in_a)              


// 	//cycle 0

//     when(local_fifo.io.out.fire()){
//         index_table.io.addr_b           := local_fifo.io.out.bits.engine_idx
//         local_idx                       := local_fifo.io.out.bits
//         state                           := sREAD       
//     }.elsewhen(global_fifo.io.out.fire()){
//         index_table.io.addr_b           := global_fifo.io.out.bits.engine_idx
//         global_idx                      := global_fifo.io.out.bits
//         state                           := sRECV
//     }.otherwise{
//         state                           := sIDLE
//     } 

// 	//cycle 1


//     when(state === sREAD){
//         io.MetaOut.bits.head.next_idx           	:= HToN(local_idx.block_idx)
//         io.MetaOut.bits.head.bitmap            	    := HToN(io.Bitmap)
//         io.MetaOut.bits.head.index            		:= HToN(index_tmp)                
//         io.MetaOut.bits.head.eth_type            	:= HToN(0x2001.U(16.W))
//         io.MetaOut.bits.head.mac_src            	:= HToN(0x1234.U(48.W))
//         io.MetaOut.bits.head.mac_dst            	:= HToN(0x4567.U(48.W))
//         io.MetaOut.bits.is_empty            	    := false.B
//         io.MetaOut.valid                            := 1.U

//         index_table.io.addr_a                       := local_idx.engine_idx
//         index_table.io.wr_en_a                      := 1.U
//         index_table.io.data_in_a.slot_idx           := index_table.io.data_out_b.slot_idx + 1.U
//         index_table.io.data_in_a.local_idx          := local_idx.block_idx
//         index_table.io.data_in_a.global_idx         := index_table.io.data_out_b.global_idx
	
//     }.elsewhen(state === sRECV){
//         when(index_table.io.data_out_b.local_idx === global_idx.block_idx){
//             state                                       := sREAD
//         }.otherwise{
//             io.MetaOut.bits.head.next_idx           	:= HToN(global_idx.block_idx)
//             io.MetaOut.bits.head.bitmap            	    := HToN(io.Bitmap)
//             io.MetaOut.bits.head.index            		:= HToN(gindex_tmp)                
//             io.MetaOut.bits.head.eth_type            	:= HToN(0x2002.U(16.W))
//             io.MetaOut.bits.head.mac_src            	:= HToN(0x1234.U(48.W))
//             io.MetaOut.bits.head.mac_dst            	:= HToN(0x4567.U(48.W))
//             io.MetaOut.bits.is_empty            	    := true.B
//             io.MetaOut.valid                            := 1.U

//             index_table.io.addr_a                       := global_idx.engine_idx
//             index_table.io.wr_en_a                      := 1.U
//             index_table.io.data_in_a.slot_idx           := index_table.io.data_out_b.slot_idx + 1.U
//             index_table.io.data_in_a.local_idx          := index_table.io.data_out_b.local_idx
//             index_table.io.data_in_a.global_idx         := global_idx.block_idx

//         }
//     }

// }

// package p4nic

// import chisel3._
// import chisel3.util._
// import common.axi._
// import chisel3.experimental.ChiselEnum
// import common.storage._
// import qdma._
// import common._
// import common.connection.SimpleRouter
// import common.connection.XArbiter


// class NetworkStackSpa extends Module {
//     val io = IO(new Bundle {
//         // Net data
//         val NetTx      = (Decoupled(new AXIS(512)))
//         val NetRx      = Flipped(Decoupled(new AXIS(512)))
//         // User data
//         val DataTx      = Flipped(Decoupled(UInt(512.W)))
//         val DataRx      = (Decoupled(UInt(512.W)))
//         //write cmd
//         val wrMemCmd    = (Decoupled(new PacketRequest))  //addr(64bit),  size(32bit),   callback=0(64bit)

//         //config
//         val rxIdxInitAddr = Input(UInt(64.W))
//         val rxDataInitAddr = Input(UInt(64.W))
//         val idxTotalLen = Input(UInt(32.W)) 
//         val nodeRank    = Input(UInt(32.W))
//         val engineRand  = Input(UInt(32.W)) 
//         val RxIdxDepth  = Input(UInt(32.W)) 
//         val IdxTransNum = Input(UInt(32.W))
//         val rfinal  		= Output(Bool())
//     })
    

//     //net packet:
//     //Ether(14B) Index(4B) Bitmap(4B) Next_id(4B) Data(128B) = 26B + 128B = 208bit + 1024bit

//     ToZero(io.wrMemCmd.valid)
//     ToZero(io.wrMemCmd.bits)
//     //TX
//     val data_split = Module(new DataSplit())


//     val gen_header = (Module(new GenHeader()))//Seq.fill(32)
//     val tx_pkg_gen = Module(new NetTxPkg())
//     val eth_lshift = Module(new LSHIFT(26,512)) 
//     val tx_add_eth = Module(new NetTxAddEthSpa())

//     //RX
//     val rx_process = Module(new RXProcess())
//     val eth_rshift = Module(new RSHIFT(26,512))
//     val rx_write_data = Module(new RXWriteData())


//     io.rfinal					:= 	rx_write_data.io.rfinal
//     //TX

//     data_split.io.DataIn        <>  io.DataTx
// 	data_split.io.IndexOut      <>  gen_header.io.LocalIndex
//     data_split.io.DataOut       <>  tx_pkg_gen.io.DataTx
//     data_split.io.TotalLen      <>  io.idxTotalLen
//     data_split.io.IdxTransNum   <>  io.IdxTransNum


//     gen_header.io.GlobalIndex    <>  rx_process.io.GlobalIndex
//     gen_header.io.Bitmap        <>  io.nodeRank
//     gen_header.io.MetaOut       <>  tx_add_eth.io.MetaIn

//     eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
//     tx_add_eth.io.DataIn        <>  eth_lshift.io.out
//     io.NetTx                    <>  tx_add_eth.io.DataOut


//     //RX
//     rx_process.io.NetRxIn       <>  io.NetRx

//     eth_rshift.io.in            <>  rx_process.io.NetRxOut

//     rx_write_data.io.NetRxIn    <>  eth_rshift.io.out
//     rx_write_data.io.GlobeIndex <>  rx_process.io.GlobeIndex2
//     rx_write_data.io.DataOut    <>  io.DataRx
//     rx_write_data.io.wrMemCmd   <>  io.wrMemCmd
//     rx_write_data.io.rxIdxInitAddr  <>  io.rxIdxInitAddr
//     rx_write_data.io.rxDataInitAddr <>  io.rxDataInitAddr
//     rx_write_data.io.IdxDepth   <>  io.RxIdxDepth


// }

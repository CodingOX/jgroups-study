//package club.hicode.blocks;
//
//import org.jgroups.Channel;
//import org.jgroups.JChannel;
//import org.jgroups.blocks.GroupRequest;
//import org.jgroups.blocks.RpcDispatcher;
//import org.jgroups.util.RspList;
//import org.jgroups.util.Util;
//
///**
// * x
// *
// * @author Liu Chunfu
// * @date 2019-01-17 11:06
// **/
//public class Blocks1 {
//
//    JChannel channel;
//    RpcDispatcher disp;
//    RspList rsp_list;
//
//    public int print(int number) {
//        System.out.println("print(" + number + ")");
//        return number * 2;
//    }
//
//    public void start() throws Exception {
//        channel=new JChannel();
//        disp=new RpcDispatcher(channel,new );
//        channel.connect("RpcDispatcherTestGroup");
//        for(int i=0; i < 100; i++) {
//            Util.sleep(1000);
//            rsp_list=disp.callRemoteMethods(null, "print", new Integer(i),
//                    GroupRequest.,
//                    0);
//            System.out.println("Responses: " + rsp_list);
//        }
//        channel.close();
//    }
//}

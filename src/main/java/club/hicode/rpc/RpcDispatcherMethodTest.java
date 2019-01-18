package club.hicode.rpc;

import org.jgroups.JChannel;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

/**
 * RpcAyncInvoker
 *
 * @author Liu Chunfu
 * @date 2019-01-17 19:39
 **/
public class RpcDispatcherMethodTest {
    JChannel channel;
    RpcDispatcher disp;
    RspList rsp_list;
    //String props; // set by application

    public static int print(int number) throws Exception {
        System.err.println("invoke...2222");
        return number * 2;
    }

    public void start() throws Exception {

        channel = new JChannel();
        disp = new RpcDispatcher(channel, this);
        channel.connect("RpcDispatcherTestGroup");

    }

    public static void main(String[] args) throws Exception {
        new RpcDispatcherMethodTest().start();
    }
}
package club.hicode.sync;

import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;

/**
 * RpcAyncInvoker
 *
 * @author Liu Chunfu
 * @date 2019-01-17 19:39
 **/
public class RpcAsyncTest {
    JChannel channel;
    RpcDispatcher disp;
    RspList rsp_list;
    //String props; // set by application

    public static int print(int number) throws Exception {
        System.err.println("invoke...2222");
        ThreadUtil.sleep(3000);
        return number * 2;
    }

    public void start() throws Exception {

        channel = new JChannel();
        disp = new RpcDispatcher(channel, this);
        channel.connect("RpcDispatcherTestGroup");

    }

    public static void main(String[] args) throws Exception {
        new RpcAsyncTest().start();
    }
}
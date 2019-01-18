package club.hicode.sync.demo;

import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;

/**
 * RpcAsyncInvoker
 *
 * @author Liu Chunfu
 * @date 2019-01-17 19:39
 **/
public class RpcAsyncTest {
    /**
     * 此处的 channel 必须是局部变量，
     */
    JChannel channel;
    RpcDispatcher disp;

    /**
     * invoke by others
     *
     * @param number
     * @return
     * @throws Exception
     */
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
package club.hicode.rpc;

import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.util.List;

/**
 * RpcAyncInvoker
 *
 * @author Liu Chunfu
 * @date 2019-01-17 19:39
 **/
public class RpcDispatcherTest {
    JChannel channel;
    RpcDispatcher disp;
    RspList rsp_list;
    //String props; // set by application

    public static int print(int number) throws Exception {
        System.err.println("invoke...");
        ThreadUtil.sleep(3000);
        return number * 3;
    }

    public void start() throws Exception {
        //打印 print
        MethodCall call = new MethodCall(getClass().getMethod("print", int.class));
        //请求所有的程序
        RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 5000);

        channel = new JChannel();
        disp = new RpcDispatcher(channel, this);
        channel.connect("RpcDispatcherTestGroup");

        for (int i = 0; i < 10; i++) {
            Util.sleep(100);
            call.setArgs(i);
            rsp_list = disp.callRemoteMethods(null, call, opts);
            System.out.println("result is :");
            //获取处理的结果
            List results = rsp_list.getResults();
            results.stream().forEach(System.out::println);
            System.out.println("Responses: " + rsp_list);
        }
        Util.close(disp, channel);
    }

    public static void main(String[] args) throws Exception {
        new RpcDispatcherTest().start();
    }
}
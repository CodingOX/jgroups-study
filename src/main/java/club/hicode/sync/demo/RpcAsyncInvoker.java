package club.hicode.sync.demo;

import org.jgroups.JChannel;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RpcAsyncInvoker
 *
 * @author Liu Chunfu
 * @date 2019-01-17 19:39
 **/
public class RpcAsyncInvoker {
    JChannel channel;
    RpcDispatcher disp;
    RspList rsp_list;
    //String props; // set by application

    public static int print(int number) throws Exception {
        System.err.println("invoke...");
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
            //异步调用，不会每一个都去阻塞，实际开发中推荐此用法！
            CompletableFuture<RspList<Object>> rspListCompletableFuture = disp.callRemoteMethodsWithFuture(null, call, opts);

            rspListCompletableFuture.thenAccept(x -> {
                List<Object> results = x.getResults();
                results.stream().forEach(System.out::println);
            });
        }
        Util.close(disp, channel);
    }

    public static void main(String[] args) throws Exception {
        new RpcAsyncInvoker().start();
    }
}
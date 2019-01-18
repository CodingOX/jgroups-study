package club.hicode.sync.program;

import org.jgroups.JChannel;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 容器同步
 *
 * @author Liu Chunfu
 * @date 2019-01-18 10:07
 **/
public class ContainerSync {

    private static final String CHANNEL_NAME = "MIC-CONTAINER-CHANNEL";

   private RpcDispatcher dispatcher;

    public ContainerSync() {
        try {
            ContainerManager manager = new ContainerManager();
            JChannel channel = new JChannel();
            dispatcher = new RpcDispatcher(channel, manager);
            channel.connect(CHANNEL_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invokeRemote(List<String> list) throws Exception {
        MethodCall call = new MethodCall("setContainer", new Object[]{list}, new Class[]{List.class});
        RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 5000);
        CompletableFuture<RspList<Object>> future = dispatcher.callRemoteMethodsWithFuture(null, call, opts);
        future.thenAccept(rsps -> System.out.println(rsps));
    }
}

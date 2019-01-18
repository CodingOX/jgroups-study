package club.hicode.message;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

/**
 * xx
 *
 * @author Liu Chunfu
 * @date 2019-01-17 16:28
 **/
public class MessageDispatcherTest implements RequestHandler {
    JChannel channel;
    MessageDispatcher disp;
    RspList rsp_list;


    public void start() throws Exception {
        channel = new JChannel();
        disp = new MessageDispatcher(channel, this);
        channel.connect("MessageDispatcherTestGroup2");

        for (int i = 0; i < 10; i++) {
            Util.sleep(100);
            //System.out.println("Casting simple #" + i);
            byte[] payload = ("Number #" + i).getBytes();
            rsp_list = disp.castMessage(null, payload, 0, payload.length, RequestOptions.SYNC());
            System.out.println("Responses:\n" + rsp_list);
        }
        //Util.close(disp, channel);
    }

    /**
     * 无论何时收到请求，都会调用该方法
     *
     * @param msg
     * @return
     * @throws Exception
     */
    @Override
    public Object handle(Message msg) {
        System.err.println("收到请求。。handle(): " + msg.getObject());
        return "Success!";
    }

    public static void main(String[] args) {
        try {
            new MessageDispatcherTest().start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}

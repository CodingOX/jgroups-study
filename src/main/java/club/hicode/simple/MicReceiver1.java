package club.hicode.simple;

import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

/**
 * @author Liu Chunfu
 * @date 2019-01-17 16:33
 **/
public class MicReceiver1 extends ReceiverAdapter {


    private JChannel channel;

    public void start() throws Exception {
        channel = new JChannel().setReceiver(this::receive);
        channel.connect("MicChat");
    }

    @Override
    public void receive(Message msg) {
        System.out.println("接收到消息：" + msg.getObject());
        ThreadUtil.safeSleep(3000);
    }

    public static void main(String[] args) throws Exception {
        new MicReceiver1().start();
    }
}

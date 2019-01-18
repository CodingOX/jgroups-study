package club.hicode.state;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.Message;

/**
 * 状态发送其
 *
 * @author Liu Chunfu
 * @date 2019-01-17 17:00
 **/
public class StateSender {

    private static final String FLAG = "mic-state";


    public static void main(String[] args) throws Exception {
        JChannel jChannel = new JChannel();
        jChannel.connect(FLAG);
        while (true) {
            Message message = new Message(null, DateUtil.now());
            message.setFlag(Message.Flag.RSVP);
            jChannel.send(message);
            System.out.println("..");
            ThreadUtil.safeSleep(1000);
        }
    }
}

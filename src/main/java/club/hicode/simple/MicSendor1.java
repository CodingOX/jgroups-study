package club.hicode.simple;

import cn.hutool.core.date.DateUtil;
import org.jgroups.JChannel;
import org.jgroups.Message;

/**
 * 发送者
 *
 * @author Liu Chunfu
 * @date 2019-01-17 16:47
 **/
public class MicSendor1 {

    public static void main(String[] args) throws Exception {
        JChannel channel=new JChannel();
        channel.connect("MicChat");
        for(int i = 0; i < 5; i++) {
            Message message=new Message(null,"Hello:"+ DateUtil.now());
            message.setFlag(Message.Flag.RSVP);
            channel.send(message);
            System.out.println("发送。。。");
        }
    }
}

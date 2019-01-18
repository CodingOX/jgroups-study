package club.hicode.message;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.jgroups.JChannel;
import org.jgroups.Message;

/**
 * @author Liu Chunfu
 * @date 2019-01-17 19:24
 **/
public class MessageSendor {

    public static void main(String[] args) throws Exception {
        JChannel channel = new JChannel();
        channel.connect("MessageDispatcherTestGroup2");
        for (int i = 0; i < 5; i++) {
            Message message = new Message(null, DateUtil.now());
            //只有都接收了才返回
            message.setFlag(Message.Flag.RSVP);
            channel.send(message);
            ThreadUtil.sleep(1000);
        }
    }
}

package com.fate.api.admin.listener;

import com.fate.api.admin.event.MailMsgEvent;
import com.fate.api.admin.event.PublicMsgEvent;
import com.fate.api.admin.event.ShortMsgEvent;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * @program: parent
 * @description: 消息事件监听器
 * @author: chenyixin
 * @create: 2019-06-14 19:43
 **/
@Component
@Slf4j
public class MsgEventListener {
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private WxMpService wxMpService;
    @Value("${cdn.domain}")
    private String cdnDomain;
    @Value("${spring.mail.username}")
    private String sender;


    /**
     * 邮件事件监听器
     *
     * @param event
     */
    @EventListener
    @Async
    public void mailListener(MailMsgEvent event) {
        try {
            log.info("发送邮件,event={}",event);
            if (!event.isHtml() && MapUtils.isEmpty(event.getAttachments())) {
                //简单邮件
                SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
                simpleMailMessage.setFrom(sender);
                simpleMailMessage.setTo(event.getTo());
                simpleMailMessage.setSubject(event.getSubject());
                simpleMailMessage.setText(event.getContent());
                javaMailSender.send(simpleMailMessage);
            } else {
                //html或者附件邮件
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
                mimeMessageHelper.setFrom(sender);
                mimeMessageHelper.setTo(event.getTo());
                mimeMessageHelper.setSubject(event.getSubject());
                mimeMessageHelper.setText(event.getContent(), event.isHtml());
                //添加行内图片
                if (MapUtils.isNotEmpty(event.getHtmlImages())) {
                    for (Map.Entry<String, Resource> entry : event.getHtmlImages().entrySet()) {
                        mimeMessageHelper.addInline(entry.getKey(), entry.getValue());
                    }
                }
                //添加附件
                if (MapUtils.isNotEmpty(event.getAttachments())) {
                    for (Map.Entry<String, InputStreamSource> entry : event.getAttachments().entrySet()) {
                        mimeMessageHelper.addAttachment(entry.getKey(), entry.getValue());
                    }
                }
                javaMailSender.send(mimeMessage);
            }
        } catch (Exception e) {
            log.error("{}发送消息失败", event, e);
        }
    }


    /**
     * 短信事件监听器
     *
     * @param event
     */
    @EventListener
    @Async
    public void smsListener(ShortMsgEvent event) {
        try {
            log.info("发送短信了,event={}",event);

        } catch (Exception e) {
            log.error("{}发送消息失败", event, e);
        }
    }

    /**
     * 公众号消息事件监听器
     *
     * @param event
     */
    @EventListener
    @Async
    public void wxPublicListener(PublicMsgEvent event) {
        try {
            log.info("发送公众号消息,event={}",event);
            event.getOpenIds().stream().forEach(openId -> {
                //推送消息
                WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                        .toUser(openId)
                        .templateId(event.getTemplateId())
                        .data(event.getTemplateData())
                        .url(event.getUrl())//点击模版消息要访问的网址
                        .miniProgram(event.getMiniProgram())//点击模版跳转到小程序页面，优先
                        .build();

                try {
                    wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
                } catch (WxErrorException e) {
                    log.error("{}发送用户{}微信公众号消息失败", event, openId, e);
                }
            });

        } catch (Exception e) {
            log.error("{}发送消息失败", event, e);
        }
    }
}

package com.openreport.admin.controller;

import com.openreport.admin.websocket.WebSocketPushService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "WebSocket服务")
@RestController
@RequestMapping("/ws")
public class WebSocketController {

    @Autowired
    private WebSocketPushService pushService;

    @ApiOperation("获取在线连接数")
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(pushService.getOnlineCount());
    }
}

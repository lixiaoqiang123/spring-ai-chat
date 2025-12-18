package com.lxq.spring_api_chat.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询工具
 * 模拟天气查询功能（实际项目中应该调用真实的天气API）
 */
@Component
public class WeatherTool implements AgentTool {

    // 模拟天气数据
    private static final Map<String, String> WEATHER_DATA = new HashMap<>();

    static {
        WEATHER_DATA.put("北京", "晴天，温度15°C，空气质量良好");
        WEATHER_DATA.put("上海", "多云，温度18°C，有轻微雾霾");
        WEATHER_DATA.put("广州", "小雨，温度22°C，湿度较高");
        WEATHER_DATA.put("深圳", "阴天，温度20°C，空气质量优");
        WEATHER_DATA.put("杭州", "晴天，温度16°C，适合出行");
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的天气信息，包括温度、天气状况和空气质量。";
    }

    @Override
    public String getParameterDescription() {
        return "city: 要查询的城市名称，例如: '北京', '上海', '广州'";
    }

    @Override
    public String execute(String input) {
        String city = input.trim();
        String weather = WEATHER_DATA.get(city);

        if (weather != null) {
            return String.format("%s的天气: %s", city, weather);
        } else {
            return String.format("抱歉，暂时没有%s的天气数据。支持的城市有: %s",
                    city, String.join(", ", WEATHER_DATA.keySet()));
        }
    }
}

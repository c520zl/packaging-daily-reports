package com.factory.report.controller;

import com.factory.report.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 字典接口，见 API.md §5；袋型与 PRD §4.4 完全一致 */
@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private static final List<String> BAG_TYPES = List.of(
            "连体拉链直立袋", "三边封袋", "三边封拉链袋", "三边封自粘袋",
            "八边封袋", "中封袋", "中封等边风琴袋", "中封侧边风琴袋");

    @GetMapping("/{type}")
    public R<List<Map<String, String>>> dict(@PathVariable String type) {
        List<String> items = switch (type) {
            case "bagType" -> BAG_TYPES;
            case "machine" -> List.of("1号机", "2号机", "3号机", "4号机", "5号机", "6号机", "7号机", "8号机");
            case "printMachine" -> List.of("1 号印刷机（八色）", "2 号印刷机（六色）");
            case "lamMachine" -> List.of("干式复合机", "无溶剂机");
            case "material" -> List.of("PET/PE", "PET/CPP", "NY/PE", "OPP/CPP", "PET/AL/PE", "PET/VMPET/CPP");
            default -> List.of();
        };
        return R.ok(items.stream().map(s -> Map.of("key", s, "label", s)).toList());
    }
}

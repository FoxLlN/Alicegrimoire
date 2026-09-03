package org.aliceGrimoire.alicegrimoire.modifier;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

import java.util.Map;

/**
 * 数据包加载器
 * 监听 alicegrimoire/modifiers/ 目录下的所有 .json 文件
 * 格式示例：
 * {
 *   "minecraft:dirt": { "max_health": 2.0 },
 *   "minecraft:stone": { "attack_damage": 1.0, "armor": 1 }
 * }
 */
public class ModifierLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    public ModifierLoader() {
        super(GSON, "modifiers"); // 数据包路径：data/alicegrimoire/modifiers/*.json
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        ModifierManager.clear();
        LOGGER.info("开始加载改装模块");

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                // 解析 JSON：每个 key 是物品 ID，value 是属性映射
                for (Map.Entry<String, JsonElement> itemEntry : json.getAsJsonObject().entrySet()) {
                    String itemId = itemEntry.getKey();
                    ResourceLocation itemRL = ResourceLocation.parse(itemId);
                    Item item = BuiltInRegistries.ITEM.get(itemRL);

                    if (item == null || item == BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                        LOGGER.warn("未知物品在改装文件 {}: {}", fileId, itemId);
                        continue;
                    }

                    // 解析属性映射
                    Map<String, Double> stats = GSON.fromJson(itemEntry.getValue(), Map.class);
                    // 确保所有值为 Double
                    stats.replaceAll((k, v) -> v instanceof Number n ? n.doubleValue() : 0.0);

                    ModifierManager.register(item, new ModifierDefinition(stats));
                }
            } catch (Exception e) {
                LOGGER.error("改装文件解析错误 {}: {}", fileId, e.getMessage());
            }
        }

        LOGGER.info("加载了 {} 个改装模块定义", ModifierManager.getSize());
    }
}
package org.aliceGrimoire.alicegrimoire;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.*;
import org.slf4j.Logger;

/**
 * AliceGrimoire 模组的主类，使用 @Mod 注解标记为模组的主类
 * 该类负责注册模组的各种组件和事件监听器
 */
@Mod(Alicegrimoire.MODID)
public class Alicegrimoire {
    // 模组的唯一标识符
    public static final String MODID = "alicegrimoire";
    // 用于记录模组日志的日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造函数，用于初始化模组并注册各种组件
     * @param modEventBus 模组事件总线，用于注册模组组件
     * @param modContainer 模组容器，包含模组的元数据
     */
    public Alicegrimoire(IEventBus modEventBus, ModContainer modContainer) {
        
        // 注册方块到事件总线
        ModBlocks.BLOCKS.register(modEventBus);
        // 注册物品到事件总线
        ModItems.ITEMS.register(modEventBus);
        // 注册实体到事件总线
        ModEntities.ENTITIES.register(modEventBus);
        // 注册方块实体到事件总线
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // 注册创造模式标签页到事件总线
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        // 注册数据组件类型到事件总线
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        // 注册菜单类型到事件总线
        ModMenuTypes.MENUS.register(modEventBus);
        // 注册附加类型到事件总线
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 注册实体属性事件监听器
        modEventBus.addListener(this::registerAttributes);
        // 注册客户端渲染器事件监听器
        modEventBus.addListener(org.aliceGrimoire.alicegrimoire.client.ClientEvents::registerRenderers);
        // 注册客户端屏幕事件监听器
        modEventBus.addListener(org.aliceGrimoire.alicegrimoire.client.ClientEvents::registerScreens);
    }

    /**
     * 注册实体的属性
     * @param event 实体属性创建事件
     */
    private void registerAttributes(EntityAttributeCreationEvent event) {
        // 为Doll实体注册属性
        event.put(ModEntities.DOLL.get(), DollEntity.createAttributes().build());
    }
}

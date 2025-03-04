package io.github.lucaargolo.fabricvision.common.item

import io.github.lucaargolo.fabricvision.common.block.BlockCompendium
import io.github.lucaargolo.fabricvision.utils.ModIdentifier
import io.github.lucaargolo.fabricvision.utils.RegistryCompendium
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text

object ItemCompendium: RegistryCompendium<Item>(Registries.ITEM) {

    val defaultStack: ItemStack
        get() = Items.DIAMOND.asItem().defaultStack

    val items: Collection<Item>
        get() = map.values

    init {
        BlockCompendium.registerBlockItems(map)
    }

    private fun registerCreativeTab() {
        Registry.register(Registries.ITEM_GROUP, ModIdentifier("creative_tab"), FabricItemGroup
            .builder()
            .displayName(Text.literal("Fabric Vision"))
            .icon { defaultStack }
            .entries { _, entries ->
                items.forEach(entries::add)
            }
            .build()
        )
    }

    override fun initialize() {
        super.initialize()
        registerCreativeTab()
    }

    override fun initializeClient() {

    }

}
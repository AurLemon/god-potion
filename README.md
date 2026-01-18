# god-potion

闲着没事干弄得，一个 Minecraft Bukkit 插件 (1.20.1+)，加了一个能够瞬间秒杀任何实体的致命药水。

## 指令

- `/godpotion get splash` - 获取喷溅型诸神黄昏药水。
- `/godpotion get lingering` - 获取滞留型诸神黄昏药水。

## 权限

- `godpotion.admin`: 允许使用 `get` 指令获取药水。
- `godpotion.use`: 允许使用/投掷该药水。

## 安装

1. 使用 Maven 构建项目: `mvn clean package`。
2. 将生成的 `target/GodPotion-0.1.0.jar` 复制到服务器的 `plugins` 文件夹中。
3. 重启服务器。

## 构建

```bash
mvn clean package
```

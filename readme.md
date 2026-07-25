**简体中文**  | [English](readme_en.md)

<div align="center">
    <h1>LightNovelReaderPlugin</h1>
    <a><img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white&style=for-the-badge"/></a>
    <a><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-0095D5.svg?logo=kotlin&logoColor=white&style=for-the-badge"/></a>
    <a><img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white&style=for-the-badge"></a>
    <a href="http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=P__gXIArh5UDBsEq7ttd4WhIYnNh3y1t&authKey=GAsRKEZ%2FwHpzRv19hNJsDnknOc86lYzNIHMPy2Jxt3S3U8f90qestOd760IAj%2F3l&noverify=0&group_code=867785526"><img alt="QQ Group" src="https://img.shields.io/badge/QQ讨论群-867785526-brightgreen.svg?logoColor=white&style=for-the-badge"></a>
    <a href="https://discord.gg/pnf4ABmDJt"><img alt="Discord" src="https://img.shields.io/badge/Discord-JOIN-4285F4.svg?logo=discord&logoColor=white&style=for-the-badge"></a>
    <a href="https://t.me/lightnoble"><img alt="Discord" src="https://img.shields.io/badge/Telegram-JOIN-188FCA.svg?logo=telegram&logoColor=white&style=for-the-badge"></a>
</div>

## 简介

这是用于轻小说阅读器[LightNovelReader](https://github.com/dmzz-yyhyy/LightNovelReader)的一个插件模板

LNR的插件不仅仅局限于数据源的编写, 您可以使用插件系统做几乎任何的事情。比如我们的Js数据源解析功能本质上就是一个插件。通过插件您可以自定义内容控件, 修改软件逻辑, 处理软件内数据, 通过文本后处理系统对几乎整个软件的文本进行处理, 又或者是添加其他编程语言的插件解析支持等等。这完全取决于您的想象力。对于您必须的api，您可以前往原仓库处提交issue, 我们会尽量满足api的开放需求。

我们非常欢迎对于插件的开发, 您可以通过以下方式联系我们

- 在 [**此处**](https://github.com/dmzz-yyhyy/LightNovelReader/issues/new/choose) 提交一个 Bug
  反馈或新功能请求
- 欢迎加入 QQ 讨论群：`867785526` | [**邀请链接**](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=P__gXIArh5UDBsEq7ttd4WhIYnNh3y1t&authKey=GAsRKEZ%2FwHpzRv19hNJsDnknOc86lYzNIHMPy2Jxt3S3U8f90qestOd760IAj%2F3l&noverify=0&group_code=867785526)
- 欢迎加入 Discord 服务器：[**邀请链接**](https://discord.gg/pnf4ABmDJt)
- 欢迎加入 Telegram 讨论群组：[**邀请链接**](https://t.me/lightnoble)
  我们会尽量解决您在插件开发中遇到的问题

喜欢的话不要忘记点个star噢!

### LNR Api

这是一套适用于LightNovelReader的api
目前api的内容不多, 后续会逐渐添加, 我们会**尽量**保证该api的二进制兼容性

[Api KDoc](https://api-doc.lnr.nariko.org/)

*注意: 目前软件在加载插件时并不会加载资源文件
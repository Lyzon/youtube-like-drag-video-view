# youtube-like-drag-video-view
a view like youtube player view  


简书文章：
http://www.jianshu.com/p/62bfa86110ac

### 实现的效果图

![demo.gif](http://ac-whikwudz.clouddn.com/c72bee2243c046b52745.gif)

### 实现思路
在YouTube APP看到这个效果的时候，就觉得挺有意思的，然后就想着去实现这个效果。想了好久，想到了以下实现方案：
* 首先播放视频的View我选择了TextureView，关于TextureView可以参考一下这篇文章： [TextureView简易教程](http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2014/1213/2153.html)
* 自定义我们的YouTubeVideoView继承一个LinearLayout，里面包裹着TextureView与下方的详情页面。
* **根据手指在屏幕上的滑动距离计算并改变TextureView当前的宽、高。**
* TextureView的滑动效果我选择**通过LayoutParams动态地设置marginTop属性**达到上下滑动的效果。
* 在最小化的时候，先判断用户意图，如果是横向滑动的话，改marginRight/Left属性来实现横向的滑动，滑动到一定距离则隐藏整个View。
* 手指抬起后剩下的滑动效果使用属性动画来实现。
* 剩下的一些细节比如说透明度的改变，最小化时的悬浮效果，以及距离屏幕边界的距离等等，也是根据手指的滑动距离得到的。
* 所有的滑动事件的处理都在给TextureView设置的OnTouchListener里完成。

#### 其他的实现思路
* TextureView的拖动效果也可以使用Android中一个帮助拖动的类ViewDragHelper来完成，在ViewDragHelper的回调中实现与其他View的联动。
* 可以试试CoordinatorLayout，协调与联动。~

### 总结一下

这个自定义ViewGroup的的代码还有许多可以优化的地方，可是本人水平有限，做得不够好。另外，这个效果不能封装成库来使用，因为局限性还是比较多的。写这个效果，从一开始的完全没有思路，到后来一步步慢慢地实现出来。其实是非常有成就感的一件事情。这次是我第一次写博客，有不好的地方请批评指正。非常感谢，给个star是对我最大的鼓励，再次感谢。

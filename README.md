# VerticalDrawerLayout
一个垂直方向的DrawerLayout,抽屉从上向下展开
<table sytle="border: 0px;">
<tr>
<td><img width="200px" src="https://github.com/corerzhang/VerticalDrawerLayout/raw/master/screenshots/demo.gif" /></td>
</tr>
</table>

```xml  
  <me.corer.verticaldrawerlayout.VerticalDrawerLayout
          android:id="@+id/vertical"
          android:layout_width="match_parent"
          android:layout_height="match_parent">

          <LinearLayout
              android:id="@+id/content"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:orientation="vertical"
              />

          <LinearLayout
              android:id="@+id/drawer"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:background="@android:color/white"
              android:orientation="vertical"/>

  </me.corer.verticaldrawerlayout.VerticalDrawerLayout>
```





// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewGroup;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaEdge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class IncrementalMountFromExtensionTest {
  private ComponentContext mContext;
  private TestComponentsLogger mComponentsLogger;
  private boolean configValue;

  @Before
  public void setup() {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
    configValue = ComponentsConfiguration.useIncrementalMountExtension;
    ComponentsConfiguration.useIncrementalMountExtension = true;
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useIncrementalMountExtension = configValue;
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  public void testIncrementalMountVerticalViewStackScrollUp() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  @Test
  public void testIncrementalMountVerticalViewStackScrollDown() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a horizontal stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountHorizontalViewStack() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(-10, 0, -5, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(5, 0, 15, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(15, 0, 25, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(20, 0, 30, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type.
   */
  @Test
  public void testIncrementalMountVerticalDrawableStack() {
    final TestComponent child1 = TestDrawableComponent.create(mContext).build();
    final TestComponent child2 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  /** Tests incremental mount behaviour of a view mount item in a nested hierarchy. */
  @Test
  public void testIncrementalMountNestedView() {
    final TestComponent child = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .wrapInView()
                    .paddingPx(ALL, 20)
                    .child(Wrapper.create(c).delegate(child).widthPx(10).heightPx(10))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 20), true);
    assertThat(child.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 40), true);
    assertThat(child.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(30, 0, 50, 40), true);
    assertThat(child.isMounted()).isFalse();
  }

  /**
   * Verify that we can cope with a negative padding on a component that is wrapped in a view (since
   * the bounds of the component will be larger than the bounds of the view).
   */
  @Test
  public void testIncrementalMountVerticalDrawableStackNegativeMargin() {
    final TestComponent child1 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(child1)
                            .widthPx(10)
                            .heightPx(10)
                            .clickHandler(c.newEventHandler(1))
                            .marginDip(YogaEdge.TOP, -10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
  }

  /** Tests incremental mount behaviour of overlapping view mount items. */
  @Test
  public void testIncrementalMountOverlappingView() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(child1)
                            .positionType(ABSOLUTE)
                            .positionPx(TOP, 0)
                            .positionPx(LEFT, 0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Wrapper.create(c)
                            .delegate(child2)
                            .positionType(ABSOLUTE)
                            .positionPx(TOP, 5)
                            .positionPx(LEFT, 5)
                            .widthPx(10)
                            .heightPx(10))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(5, 5, 10, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(10, 10, 15, 15), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 20, 20), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
  }

  @Test
  public void testChildViewGroupIncrementallyMounted() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getChildCount()).thenReturn(3);

    final LithoView childView1 = getMockLithoViewWithBounds(new Rect(5, 10, 20, 30));
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final LithoView childView2 = getMockLithoViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final LithoView childView3 = getMockLithoViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            TestViewComponent.create(mContext).testView(mountedView), true);

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 40, 40), true);

    verify(childView1).notifyVisibleBoundsChanged();
    verify(childView2).notifyVisibleBoundsChanged();
    verify(childView3).notifyVisibleBoundsChanged();
  }

  @Test
  public void testChildViewGroupAllIncrementallyMountedNotProcessVisibilityOutputs() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getLeft()).thenReturn(0);
    when(mountedView.getTop()).thenReturn(0);
    when(mountedView.getRight()).thenReturn(100);
    when(mountedView.getBottom()).thenReturn(100);
    when(mountedView.getChildCount()).thenReturn(3);

    final LithoView childView1 = getMockLithoViewWithBounds(new Rect(5, 10, 20, 30));
    when(childView1.getTranslationX()).thenReturn(5.0f);
    when(childView1.getTranslationY()).thenReturn(-10.0f);
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final LithoView childView2 = getMockLithoViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final LithoView childView3 = getMockLithoViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            TestViewComponent.create(mContext).testView(mountedView), true);

    // Can't verify directly as the object will have changed by the time we get the chance to
    // verify it.
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 15, 20))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView1)
        .notifyVisibleBoundsChanged(any(Rect.class), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 40, 50))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView2)
        .notifyVisibleBoundsChanged(any(Rect.class), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 20, 25))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView3)
        .notifyVisibleBoundsChanged(any(Rect.class), eq(true));

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 100, 100), true);

    verify(childView1).notifyVisibleBoundsChanged();
    verify(childView2).notifyVisibleBoundsChanged();
    verify(childView3).notifyVisibleBoundsChanged();
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  public void testIncrementalMountDoesNotCauseMultipleUpdates() {
    final TestComponent child1 = create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isTrue();
    assertThat(child1.wasOnUnmountCalled()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();

    child1.resetInteractions();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();

    assertThat(child1.wasOnBindCalled()).isFalse();
    assertThat(child1.wasOnMountCalled()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isFalse();
    assertThat(child1.wasOnUnmountCalled()).isFalse();
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type
   * after unmountAllItems was called.
   */
  @Test
  public void testIncrementalMountAfterUnmountAllItemsCall() {
    final TestComponent child1 = TestDrawableComponent.create(mContext).build();
    final TestComponent child2 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.unmountAllItems();
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
  }

  /**
   * Tests incremental mount behaviour of a nested Litho View. We want to ensure that when a child
   * view is first mounted due to a layout pass it does not also have notifyVisibleBoundsChanged
   * called on it.
   */
  @Test
  public void testIncrementalMountAfterLithoViewIsMounted() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    final ViewGroupWithLithoViewChildren viewGroup =
        new ViewGroupWithLithoViewChildren(mContext.getAndroidContext());
    viewGroup.addView(lithoView);

    final LithoView lithoViewParent =
        ComponentTestHelper.mountComponent(
            TestViewComponent.create(mContext, true, true, true, true).testView(viewGroup), true);

    // Mount views with visible rect
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, 0, 100, 1000), true);
    verify(lithoView).notifyVisibleBoundsChanged();
    reset(lithoView);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    // Unmount views with visible rect outside
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, -10, 100, -5), true);
    verify(lithoView, never()).notifyVisibleBoundsChanged();
    reset(lithoView);
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    // Mount again with visible rect
    lithoViewParent.getComponentTree().mountComponent(new Rect(0, 0, 100, 1000), true);

    // Now LithoView notifyVisibleBoundsChanged should not be called as the LithoView is mounted
    // when
    // it is laid out and therefore doesn't need mounting again in the same frame
    verify(lithoView, never()).notifyVisibleBoundsChanged();
  }

  private static LithoView getMockLithoViewWithBounds(Rect bounds) {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getLeft()).thenReturn(bounds.left);
    when(lithoView.getTop()).thenReturn(bounds.top);
    when(lithoView.getRight()).thenReturn(bounds.right);
    when(lithoView.getBottom()).thenReturn(bounds.bottom);
    when(lithoView.getWidth()).thenReturn(bounds.width());
    when(lithoView.getHeight()).thenReturn(bounds.height());
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    return lithoView;
  }

  private static class TestLithoView extends LithoView {
    private final Rect mPreviousIncrementalMountBounds = new Rect();

    public TestLithoView(Context context) {
      super(context);
    }

    @Override
    public void notifyVisibleBoundsChanged(Rect visibleRect, boolean processVisibilityOutputs) {
      System.out.println("performIncMount on TestLithoView");
      mPreviousIncrementalMountBounds.set(visibleRect);
    }

    private Rect getPreviousIncrementalMountBounds() {
      return mPreviousIncrementalMountBounds;
    }

    @Override
    public boolean isIncrementalMountEnabled() {
      return true;
    }
  }
}

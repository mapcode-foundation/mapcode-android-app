/*
 * Copyright (C) 2022, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.util

import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState

@OptIn(ExperimentalPagerApi::class)
suspend fun PagerState.animateScrollToNextPage() {
    if (currentPage == pageCount - 1) {
        return
    }

    animateScrollToPage(currentPage + 1)
}

@OptIn(ExperimentalPagerApi::class)
suspend fun PagerState.animateScrollToPreviousPage() {
    if (currentPage == 0) {
        return
    }

    animateScrollToPage(currentPage - 1)
}

@OptIn(ExperimentalPagerApi::class)
fun PagerState.isLastPage(): Boolean = this.currentPage == this.pageCount - 1
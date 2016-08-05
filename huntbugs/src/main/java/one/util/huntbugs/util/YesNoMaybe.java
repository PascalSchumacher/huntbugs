/*
 * Copyright 2016 HuntBugs contributors
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
package one.util.huntbugs.util;

/**
 * @author shustkost
 *
 */
public enum YesNoMaybe {
    YES, MAYBE, NO;
    
    public boolean yes() {
        return this == YES;
    }
    
    public boolean no() {
        return this == NO;
    }

    public static YesNoMaybe of(boolean b) {
        return b ? YES : NO;
    }
}

/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "uid_fetcher.h"

#include "procfs_files.h"

namespace profiler {

int UidFetcher::GetUid(int pid) {
  ProcfsFiles procfs_files;
  std::string uid_string;
  if (UidFetcher::GetUidStringFromPidFile(
          procfs_files.GetProcessStatusFilePath(pid), &uid_string)) {
    return atoi(uid_string.c_str());
  }
  return -1;
}
}  // namespace profiler
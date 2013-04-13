# GitHub Project Relations Visualization

A visualization of the relation between GitHub projects, based on common
collaborators. Interactive web interface.

## Visualization

 * Search for projects
 * Search for languages
 * Time-run
 * Fix on project / possible filter for it
 * Window sizing
 * Link value needs label

 * Monthly periods iso weekly?
 * Use case classes iso tuples?

## Build & Run

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.

## License

Check the AUTHORS.txt file for a list of the copyright holders.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

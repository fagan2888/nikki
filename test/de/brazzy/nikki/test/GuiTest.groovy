package de.brazzy.nikki.test

/*   
 *   Copyright 2010 Michael Borgwardt
 *   Part of the Nikki Photo GPS diary:  http://www.brazzy.de/nikki
 *
 *  Nikki is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Nikki is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Nikki.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.log4j.Logger;

import de.brazzy.nikki.Nikki;
import de.brazzy.nikki.model.Directory;
import de.brazzy.nikki.model.NikkiModel;
import de.brazzy.nikki.util.ParserFactory;
import de.brazzy.nikki.util.TimezoneFinder;
import de.brazzy.nikki.view.NikkiFrame;

/**
 * @author Michael Borgwardt
 */
abstract class GuiTest extends AbstractNikkiTest {
    
    TestDialogs dialogs
    NikkiModel model
    NikkiFrame view
    Nikki nikki
    final long baseTime = System.currentTimeMillis()-10000000    
    
    public void setUp() {
        super.setUp()
        dialogs = new TestDialogs()
        nikki = new Nikki()
        ParserFactory pf = new ParserFactory();
        nikki.build(GuiTest.class, dialogs, new TimezoneFinder(), pf)
        model = nikki.model
        for(Directory d in model){
            model.remove(d)            
        }
        view = nikki.view
    }
    
    public void tearDown() {
        assertTrue(dialogs.isQueueEmpty())
        view.frame.dispose()
        super.tearDown()
    }   
}





/*
 * Copyright 2014-2015 Branko Juric, Brady Wood
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

package gwen.eval

import gwen.errors._
import gwen.Predefs.FileIO
import gwen.Predefs.Kestrel
import java.io.File
import java.util.NoSuchElementException
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class featureStreamTest extends FlatSpec with Matchers {
  
  val featureStream = new FeatureStream(Nil)
  
  val rootDir = new File("target" + File.separator + "features") tap { _.mkdirs() }
  
  "Directory with no feature files" should "result in empty suite" in {
    featureStream.read(createDir("dir1"), None).size should be (0)
  }
  
  "Directory with non feature files" should "return empty suite" in {
    createDir("dir2");
    featureStream.read(createFile("dir2/file.meta"), None).size should be (0)
    featureStream.read(createFile("dir2/file.text"), None).size should be (0)
    featureStream.read(createFile("dir2/feature"), None).size should be (0)
  }
  
  it should "not find feature files above the specified directory" in {
    createDir("dir3")
    createFile("dir3/file.feature")
    createFile("dir3/file.meta")
    featureStream.read(createDir("dir3/dir4"), None).size should be (0)
  }
  
  "1 input feature file with no meta" should "return the 1 feature file only" in {
    createDir("dir4")
    val featureFile = createFile("dir4/file.feature")
    val suite = featureStream.read(featureFile, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(Nil, unit.metaFiles)
        unit.dataRecord should be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with one meta in same dir" should "return the 1 feature and 1 meta" in {
    val dir         = createDir("dir5")
    val featureFile = createFile("dir5/file.feature")
    val metaFile    = createFile("dir5/file.meta")
    val suite = featureStream.read(dir, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile), unit.metaFiles)
        unit.dataRecord should be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with one data file in same dir" should "return the 1 feature and 1 data file" in {
    val dir         = createDir("dir5a")
    val featureFile = createFile("dir5a/file.feature")
    val dataFile    = createDataFile("dir5a/file.csv")
    val suite = featureStream.read(dir, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(Nil, unit.metaFiles)
        assertFile(dataFile, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }

  "1 input feature file with one meta and data file in same dir" should "return the 1 feature and 1 meta and 1 data" in {
    val dir         = createDir("dir5b")
    val featureFile = createFile("dir5b/file.feature")
    val metaFile    = createFile("dir5b/file.meta")
    val dataFile    = createDataFile("dir5b/file.csv")
    val suite = featureStream.read(dir, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile), unit.metaFiles)
        assertFile(dataFile, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with one meta and data file in same dir" should "return the 1 feature and 1 meta and 1 data if data file is also passed in call" in {
    val dir         = createDir("dir5c")
    val featureFile = createFile("dir5c/file.feature")
    val metaFile    = createFile("dir5c/file.meta")
    val dataFile    = createDataFile("dir5c/file.csv")
    val suite = featureStream.read(dir, Some(dataFile))
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile), unit.metaFiles)
        assertFile(dataFile, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with one meta and 2 data files in same dir" should "should use data file passed in call" in {
    val dir          = createDir("dir5d")
    val featureFile  = createFile("dir5d/file.feature")
    val metaFile     = createFile("dir5d/file.meta")
    val dataFile1    = createDataFile("dir5d/file1.csv")
    val dataFile2    = createDataFile("dir5d/file2.csv")
    val suite = featureStream.read(dir, Some(dataFile1))
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile), unit.metaFiles)
        assertFile(dataFile1, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with one meta and two data files in same dir" should "error" in {
    val dir         =  createDir("dir5e")
    val featureFile = createFile("dir5e/file.feature")
    val metaFile    = createFile("dir5e/file.meta")
    val dataFile1    = createDataFile("dir5e/file1.csv")
    val dataFile2    = createDataFile("dir5e/file2.csv")
    intercept[AmbiguousCaseException] {
      featureStream.read(dir, None)
    }
  }
  
  "1 input feature file with 1 meta in same dir and 1 meta in parent" should "return the 1 feature and 2 meta" in {
    val dir6        =  createDir("dir6")
    val dir7        =  createDir("dir6/dir7")
    val metaFile1   = createFile("dir6/file1.meta")
    val metaFile2   = createFile("dir6/dir7/file2.meta")
    val featureFile = createFile("dir6/dir7/file.feature")
    val dataFile    = createDataFile("dir6/dir7/file.csv");
    val suite = featureStream.read(dir6, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile1, metaFile2), unit.metaFiles)
        assertFile(dataFile, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file in bottom dir with 1 meta in same dir and 1 meta in parent and 1 data file in same dir and another in sub dir" should "be ok" in {
    val dir61       =  createDir("dir61")
    val dir71       =  createDir("dir61/dir71")
    val metaFile1   = createFile("dir61/file1.meta")
    val dataFile1   = createDataFile("dir61/file.csv");
    val metaFile2   = createFile("dir61/dir71/file2.meta")
    val featureFile = createFile("dir61/dir71/file.feature")
    val dataFile2   = createDataFile("dir61/dir71/file.csv");
    val suite = featureStream.read(dir61, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile1, metaFile2), unit.metaFiles)
        assertFile(dataFile2, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file in top dir with 1 meta in same dir and 1 meta in parent and 1 data file in same dir and another in sub dir" should "be ok" in {
    val dir62       =  createDir("dir62")
    val dir72       =  createDir("dir62/dir72")
    val featureFile = createFile("dir62/file.feature")
    val metaFile1   = createFile("dir62/file1.meta")
    val dataFile1   = createDataFile("dir62/file.csv");
    val metaFile2   = createFile("dir62/dir72/file2.meta")
    val dataFile2   = createDataFile("dir62/dir72/file.csv");
    val suite = featureStream.read(dir62, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile1, metaFile2), unit.metaFiles)
        assertFile(dataFile1, new File(unit.dataRecord.get.dataFilePath))
        unit.dataRecord should not be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "1 input feature file with no meta in same dir but 1 meta in parent " should "return the 1 feature and 1 meta" in {
    val dir8        =  createDir("dir8")
    val dir9        =  createDir("dir8/dir9")
    val metaFile    = createFile("dir8/file.meta")
    val featureFile = createFile("dir8/dir9/file.feature")
    val suite = featureStream.read(dir8, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile), unit.metaFiles)
        unit.dataRecord should be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "feature file with parent directory containing meta" should "return 1 feature and accumulated meta" in {
    val dir10       =  createDir("dir10")
    val dir11       =  createDir("dir10/dir11")
    val dir12       =  createDir("dir10/dir11/dir12")
    val metaFile1   = createFile("dir10/file1.meta")
    val metaFile2   = createFile("dir10/dir11/dir12/file2.meta")
    val metaFile3   = createFile("dir10/dir11/dir12/file3.meta")
    val featureFile = createFile("dir10/dir11/dir12/file.feature")
    val suite = featureStream.read(featureFile, None)
    suite match {
      case unit #:: Stream() => 
        assertFile(featureFile, unit.featureFile)
        assertMetaFiles(List(metaFile1, metaFile2, metaFile3), unit.metaFiles.sortBy(_.getName()))
        unit.dataRecord should be (None)
      case _ =>
        fail(s"1 feature unit expected but ${suite.size} found")
    }
  }
  
  "multi suite stream" should "be read in correctly" in {
    
    val dirA           =  createDir("dirA")
    val metaFileA      = createFile("dirA/fileA.meta")
    val featureFileA   = createFile("dirA/fileA.feature")
    val dirAB          =  createDir("dirA/dirB")
    val metaFileAB     = createFile("dirA/dirB/fileAB.meta")
    val featureFileAB  = createFile("dirA/dirB/fileAB.feature")
    val dirAB1         =  createDir("dirA/dirB/dir1")
    val featureFileAB1 = createFile("dirA/dirB/dir1/fileAB1.feature")
    val dirAB2         =  createDir("dirA/dirB/dir2")
    val featureFileAB2 = createFile("dirA/dirB/dir2/fileAB2.feature")
    val metaFileAB2    = createFile("dirA/dirB/dir2/fileAB2.meta")
    val dirD           =  createDir("dirD")
    val featureFile1D  = createFile("dirD/file1D.feature")
    val featureFile2D  = createFile("dirD/file2D.feature")
    val dirDE          =  createDir("dirD/dirE")
    val metaFileDE     = createFile("dirD/dirE/fileDE.meta")
    val featureFileDE  = createFile("dirD/dirE/fileDE.feature")
    val dirF           =  createDir("dirF")
    val metaFileF      = createFile("dirF/fileF.meta")
    
    val suiteStream = featureStream.readAll(List(dirA, dirAB, dirAB1, dirAB2, featureFileAB2, dirD, featureFile2D, dirDE, dirF), None)
    val suites = suiteStream.iterator

    // dirA suite
    var suite = suites.next
    var units = suite.toList.sortBy(_.featureFile).iterator
    var unit = units.next
    assertFile(featureFileAB1, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFileAB2, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB, metaFileAB2), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFileAB, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFileA, unit.featureFile)
    assertMetaFiles(List(metaFileA), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dirAB suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileAB1, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFileAB2, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB, metaFileAB2), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFileAB, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dir AB1 suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileAB1, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dir AB2 suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileAB2, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB, metaFileAB2), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // featureFile AB2 suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileAB2, unit.featureFile)
    assertMetaFiles(List(metaFileA, metaFileAB, metaFileAB2), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dir D suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileDE, unit.featureFile)
    assertMetaFiles(List(metaFileDE), unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFile1D, unit.featureFile)
    assertMetaFiles(Nil, unit.metaFiles)
    unit.dataRecord should be (None)
    unit = units.next
    assertFile(featureFile2D, unit.featureFile)
    assertMetaFiles(Nil, unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // featureFile 2D suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFile2D, unit.featureFile)
    assertMetaFiles(Nil, unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dir DE suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    unit = units.next
    assertFile(featureFileDE, unit.featureFile)
    assertMetaFiles(List(metaFileDE), unit.metaFiles)
    unit.dataRecord should be (None)
    assertEndOfStream(units.next)
    
    // dir F suite
    suite = suites.next
    units = suite.toList.sortBy(_.featureFile).iterator
    assertEndOfStream(units.next)
    
  }
  
  private def createFile(filepath: String): File = {
    val file = new File(rootDir + File.separator + filepath.replace('/', File.separatorChar))
    file.getParentFile().mkdirs()
    file.createNewFile()
    file
  }
  
  private def createDataFile(filepath: String): File = createFile(filepath) tap { file => 
    file.writeText("col1,col1\ndata1,data2");
  }
  
  private def createDir(dirname: String): File = {
    val dir = new File(rootDir, dirname)
    dir.deleteDir()
    dir.mkdirs()
    dir
  }
  
  private def assertFile(expected: File, actual: File) {
    actual.getPath.startsWith("target") should be (true)
    actual.getPath should be (expected.getPath)
  }
  
  private def assertMetaFiles(expecteds: List[File], actuals: List[File]) {
    expecteds zip actuals foreach { case (expected, actual) =>
      val path = actual.getPath() 
      path.startsWith("target") should be (true)
      path should be (expected.getPath)
    }
  }
  
  private def assertEndOfStream(next: () => Unit) {
    intercept[NoSuchElementException] {
      next()
    }
  }
  
}
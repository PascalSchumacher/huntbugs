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
package one.util.huntbugs.db;

import java.util.HashMap;
import java.util.Map;

import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;

import one.util.huntbugs.registry.AbstractTypeDatabase;
import one.util.huntbugs.registry.anno.TypeDatabase;
import one.util.huntbugs.registry.anno.TypeDatabaseItem;

/**
 * @author Tagir Valeev
 *
 */
@TypeDatabase
public class FieldStats extends AbstractTypeDatabase<FieldStats.TypeFieldStats>{
    public static final int WRITE_CONSTRUCTOR = 0x0001;
    public static final int WRITE_CLASS = 0x0002;
    public static final int WRITE_PACKAGE = 0x0004;
    public static final int WRITE_OUTSIDE = 0x0008;
    public static final int WRITE = WRITE_CLASS | WRITE_PACKAGE | WRITE_OUTSIDE;
    public static final int READ_CLASS = 0x0100;
    public static final int READ_PACKAGE = 0x0200;
    public static final int READ_OUTSIDE = 0x0400;
    public static final int READ = READ_CLASS | READ_PACKAGE | READ_OUTSIDE;
    public static final int ACCESS = READ | WRITE;
    public static final int UNRESOLVED = 0x10000000;
    
    public FieldStats() {
        super(name -> new TypeFieldStats());
    }
    
    @Override
    protected void visitType(TypeDefinition td) {
        getOrCreate(td);
        for(MethodDefinition md : td.getDeclaredMethods()) {
            MethodBody body = md.getBody();
            if(body != null) {
                for(Instruction instr : body.getInstructions()) {
                    boolean write = false;
                    switch(instr.getOpCode()) {
                    case PUTFIELD:
                    case PUTSTATIC:
                        write = true;
                        // passthru
                    case GETFIELD:
                    case GETSTATIC:
                        FieldDefinition fd = ((FieldReference)instr.getOperand(0)).resolve();
                        if(fd == null || fd.isSynthetic())
                            break;
                        getOrCreate(fd.getDeclaringType()).link(md, fd, write);
                        break;
                    default:
                    }
                }
            }
        }
    }
    
    public int getFlags(FieldReference fr) {
        TypeFieldStats fs = get(fr.getDeclaringType());
        return fs == null ? UNRESOLVED : fs.fieldRecords.getOrDefault(fr.getName(), 0); 
    }

    @TypeDatabaseItem(parentDatabase=FieldStats.class)
    public static class TypeFieldStats {
        Map<String, Integer> fieldRecords = new HashMap<>();
        
        void link(MethodDefinition src, FieldDefinition fd, boolean write) {
            int prevStatus = fieldRecords.getOrDefault(fd.getName(), 0);
            int curStatus = prevStatus;
            if(src.getDeclaringType().isEquivalentTo(fd.getDeclaringType())) {
                curStatus |= write ? WRITE_CLASS : READ_CLASS;
                if(write && (src.isConstructor() && !fd.isStatic() || src.isTypeInitializer() && fd.isStatic())) {
                    curStatus |= WRITE_CONSTRUCTOR;
                }
            } else if(src.getDeclaringType().getPackageName().equals(fd.getDeclaringType().getPackageName())) {
                curStatus |= write ? WRITE_PACKAGE : READ_PACKAGE;
            } else {
                curStatus |= write ? WRITE_OUTSIDE : READ_OUTSIDE;
            }
            if(prevStatus != curStatus) {
                fieldRecords.put(fd.getName(), curStatus);
            }
        }
    }
}

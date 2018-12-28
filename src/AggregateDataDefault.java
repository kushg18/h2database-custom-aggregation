/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.aggregate;

import java.util.ArrayList;

import org.h2.engine.Database;
import org.h2.expression.aggregate.Aggregate.AggregateType;
import org.h2.message.DbException;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDouble;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

/**
 * Data stored while calculating an aggregate.
 */
class AggregateDataDefault extends AggregateData {

    private final AggregateType aggregateType;
    private long count;
    private Value value;
    private double m2, mean;
    /**
     * DBSI H2 Assignment 3 Start
     */
    private ArrayList<Value> pivot = new ArrayList<>();

    /**
     * @param aggregateType the type of the aggregate operation
     */
    AggregateDataDefault(AggregateType aggregateType) {
        this.aggregateType = aggregateType;
    }

    @Override
    void add(Database database, int dataType, Value v) {
        if (v == ValueNull.INSTANCE) {
            return;
        }
        count++;
        switch (aggregateType) {
        case SUM:
            if (value == null) {
                value = v.convertTo(dataType);
            } else {
                v = v.convertTo(value.getType());
                value = value.add(v);
            }
            break;
        case AVG:
            if (value == null) {
                value = v.convertTo(DataType.getAddProofType(dataType));
            } else {
                v = v.convertTo(value.getType());
                value = value.add(v);
            }
            break;
        case MIN:
            if (value == null || database.compare(v, value) < 0) {
                value = v;
            }
            break;
        case MAX:
            if (value == null || database.compare(v, value) > 0) {
                value = v;
            }
            break;
        case STDDEV_POP:
        case STDDEV_SAMP:
        case VAR_POP:
        case VAR_SAMP: {
            // Using Welford's method, see also
            // http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
            // http://www.johndcook.com/standard_deviation.html
            double x = v.getDouble();
            if (count == 1) {
                mean = x;
                m2 = 0;
            } else {
                double delta = x - mean;
                mean += delta / count;
                m2 += delta * (x - mean);
            }
            break;
        }
        case BOOL_AND:
            v = v.convertTo(Value.BOOLEAN);
            if (value == null) {
                value = v;
            } else {
                value = ValueBoolean.get(value.getBoolean() && v.getBoolean());
            }
            break;
        case BOOL_OR:
            v = v.convertTo(Value.BOOLEAN);
            if (value == null) {
                value = v;
            } else {
                value = ValueBoolean.get(value.getBoolean() || v.getBoolean());
            }
            break;
        case BIT_AND:
            if (value == null) {
                value = v.convertTo(dataType);
            } else {
                value = ValueLong.get(value.getLong() & v.getLong()).convertTo(dataType);
            }
            break;
        case BIT_OR:
            if (value == null) {
                value = v.convertTo(dataType);
            } else {
                value = ValueLong.get(value.getLong() | v.getLong()).convertTo(dataType);
            }
            break;
        case PIVOT:
        	System.out.println("In add!!");
        	if (value == null) {
        		pivot.add(v.convertTo(dataType));
            } else {
            	v = v.convertTo(value.getType());
            	pivot.add(v);
            }
            break;
        default:
            DbException.throwInternalError("type=" + aggregateType);
        }
    }

    @Override
    Value getValue(Database database, int dataType) {
        Value v = null;
        switch (aggregateType) {
        case SUM:
        case MIN:
        case MAX:
        case BIT_OR:
        case BIT_AND:
        case BOOL_OR:
        case BOOL_AND:
            v = value;
            break;
        case AVG:
            if (value != null) {
                v = divide(value, count);
            }
            break;
        case STDDEV_POP: {
            if (count < 1) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(Math.sqrt(m2 / count));
            break;
        }
        case STDDEV_SAMP: {
            if (count < 2) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(Math.sqrt(m2 / (count - 1)));
            break;
        }
        case VAR_POP: {
            if (count < 1) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(m2 / count);
            break;
        }
        case VAR_SAMP: {
            if (count < 2) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(m2 / (count - 1));
            break;
        }
        case PIVOT:
        	System.out.println("In get value!!");
        	if (count < 1) {
        		return ValueNull.INSTANCE;
            } else {
            	int lengthOfList = pivot.size();
            	v = pivot.get(lengthOfList / 2);
            }
        	break;
    	/**
         * DBSI H2 Assignment 3 End
         */
        default:
            DbException.throwInternalError("type=" + aggregateType);
        }
        return v == null ? ValueNull.INSTANCE : v.convertTo(dataType);
    }

    private static Value divide(Value a, long by) {
        if (by == 0) {
            return ValueNull.INSTANCE;
        }
        int type = Value.getHigherOrder(a.getType(), Value.LONG);
        Value b = ValueLong.get(by).convertTo(type);
        a = a.convertTo(type).divide(b);
        return a;
    }

}

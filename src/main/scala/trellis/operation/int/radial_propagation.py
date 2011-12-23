#------------------------------------------------------------------------------------------------------  STANDARD PRELIMINARIES
import sys, string, os, win32com.client, math
gp = win32com.client.Dispatch("esriGeoprocessing.GpDispatch.1")
gp.SetProduct("ArcView")

#---------------------------------------------------------------------------------------- SET UP FILE FOR DIAGNOSTIC TEXT OUPUT
sys.stdout = open('c:/projects/ariadnes/ariadnes_log.txt', 'a')                                             







#---------------------------------------------------------------------------  DEFINE THE SPREAD FUNCTION THAT DOES ALL THE WORK
def spread(HowManyRows,HowManyColumns,OldMatrix,FrictionMatrix,VMatrix,HMatrix,TopoMatrix):


    #------------------------------------------------------------  GET THE USER-SPECIFIED MAXIMUM DISTANCE LIMIT IN CELL WIDTHS
    HowFar = int(sys.argv[5])
    if HowFar < 0: HowFar = 10



    #-------------------------------------------------------------------------------------------------- INITIALIZE SOME SCALARS    
    Resolution      = 20.0              # Grid cell width                              
    Run             = 1.0               # Number of cell widths between centers of orthogonally or diagonally adjacent cells
    Friction        = 1.0               # Travelcost distance centers of orthogonally or diagonally adjacent cells
    AddedCost       = 1.0               # Slope-weighted travelcost distance centers of orthogonally or diagonally adjacent cells
    Reach           = 1.0               # Inclined travelcost distance centers of orthogonally or diagonally adjacent cells
    NeighborV       = 0.0               # Vertical component of a neighbor's incoming angle
    NeighborH       = 0.0               # Horizontal component of a neighbor's incoming angle
    NextBeep        = 10                # Starting percentage figure for progress report
    PopperFriction  = 1.0               # Friction value of vcurrent popper cell
    PopperV         = 0.0               # Vertical component of a popper cell's incoming angle
    PopperH         = 0.0               # Horizontal component of a popper cell'sincoming angle



    #--------------------------------------------------------- INITIALIZE SOME ARRAYS AND LOAD THE POPPER LIST WITH TARGET CELLS
    CompassCode = [32,64,128,16,0,1,8,4,2]
    PopperList      = [[0,0,0]]   
    for R in range(HowManyRows):
        for C in range(HowManyColumns):                                 
            if FrictionMatrix[R][C] < 1:
                FrictionMatrix[R][C]    = 1.0
            if OldMatrix[R][C] >= 0:
                PopperList.append([OldMatrix[R][C],R,C])                    # Put each target cell onto a "popper" list      
                NewMatrix[R][C]     = OldMatrix[R][C]                       # and set its distance to its headstart value
            else:   NewMatrix[R][C] = HowFar                                # while non-target distances are set high
    PopperList[0:1] = []                               
    PopperList.sort()                                                       #   Sort the poppers by distance



    #----------------------------------------------------------------------------------------------- LOOP THROUGH THE POPPER LIST                                 
    for NextPopper in PopperList:
        PopperDistance  = NextPopper[0]                                     # Accumulated travelcost distance the current popper
        if PopperDistance > HowFar: break                                   # Limit distance to specified maximum (albeit crudely)
        PopperRow       = NextPopper[1]                                     # Row number of the current popper
        PopperColumn    = NextPopper[2]                                     # Column number of the current popper
        PopperPosition  = PopperList.index(NextPopper)                      # Position of the current popper in the popper list
        PopperFriction  = FrictionMatrix[PopperRow][PopperColumn]           # Incremental travelcost friction of the current popper                                                       
        PopperV         = VMatrix[PopperRow][PopperColumn]                  # Vertical component of the popper's incoming angle                                                                        
        PopperH         = HMatrix[PopperRow][PopperColumn]                  # Horizontal component of the popper's incoming angle

        PercentComplete = (PopperDistance * 100) / HowFar                   # Report progress               
        if (PercentComplete) > NextBeep:
            gp.AddMessage(str(NextBeep) + ' percent done')
            NextBeep = NextBeep + 10



        #---------------------------------------------------------------------------------- LOOP THROUGH A GIVEN POPPER'S NEIGHBORS
        Top             = max(PopperRow-1, 0)                                                   # Establish popper's neighborhood
        Bottom          = min(PopperRow+1, HowManyRows-1) 
        Left            = max(PopperColumn-1, 0)
        Right           = min(PopperColumn+1, HowManyColumns-1)
        for Row in range(Top,Bottom+1):                 
            NeighborV = PopperRow - Row                                     
            for Column in range(Left,Right+1):
                #if TopoMatrix[Row][Column] <= TopoMatrix[PopperRow][PopperColumn]: continue    # Limit dissipation to uhill only
                if Row == PopperRow and Column == PopperColumn: continue                        # Skip neighborhood center
                NeighborH = Column - PopperColumn                           


                #------------------------------------------------------------- CALCULATE TRAVELCOST DISTANCE FROM POPPER TO NEIGHBOR
                if Row == PopperRow or Column == PopperColumn:             
                    Horizontal  = 1.0
                    Run         = Resolution * Horizontal
                    Friction    = (PopperFriction + FrictionMatrix[Row][Column]) / 2
                else:
                    Horizontal  = math.sqrt(2)
                    Run         = Resolution * Horizontal
                    Friction    = (PopperFriction + FrictionMatrix[Row][Column]) / Horizontal


                #-------------------------------------------  USE THE 3D SURFACE TO ACHIEVE PLUME-LIKE DISSIPATION OF DISTANCE VALUES
                #                                             IN AN MANNER THAT I EXPECT TO REFINE FURTHER BUT WILL LEAVE AS IS FOR NOW
                Rise        = (TopoMatrix[Row][Column] - TopoMatrix[PopperRow][PopperColumn]) * 13      
                Tangent     = Rise / Run                               
                #if Tangent < 0: Tangent = -Tangent          # This removes any distinction between uphill and downhill motion                                         
                Reach       = Run + (Run * Tangent)                                                     
                Radians     = math.atan2(Rise,Run)
                Reach       = Reach / math.cos(Radians)                                                 
                AddedCost   = Friction  * (Reach / Run)
                #AddedCost   = Friction                       # This just cancels the above plume effect


                #-------------------------------------------------------------------  CALCULATE NEIGHBOR'S N-NE-E-SE-S-SW-W-NW BEARING
                Direction   = CompassCode[(3 * (Row + 1 - PopperRow)) + Column + 1 - PopperColumn]      


                #------------------------------------  CALCULATE NEIGHBOR'S ACCUMULATED TRAVEL COST DISTANCE WITH ARIADNE COMPENSATION
                Distance    = PopperDistance + AddedCost                                 
                TurnWeight =(math.sqrt(math.pow((PopperV+NeighborV),2)+math.pow((PopperH+NeighborH),2)))-(math.sqrt(math.pow(PopperV,2)+math.pow(PopperH,2)))
                TurnWeight = TurnWeight / math.sqrt(math.pow(NeighborV,2)+math.pow(NeighborH,2))
                if TurnWeight < 0.82: TurnWeight = 1.0
                if PopperDistance <> 0.0: Distance      = PopperDistance + (AddedCost * TurnWeight)              


                #------------------------------------------------  IF NEIGHBOR'S CALCULATED DISTANCE EXCEEDS AN EARLIER ONE, FORGET IT
                if  ( Distance  >  NewMatrix[Row][Column] ): continue



                #------------------------------------ REMEMBER THE HORIZONTAL AND VERTICAL COMPONENTS OF THE NEIGHBOR'S INCOMING ANGLE
                VMatrix[Row][Column]    = PopperV + NeighborV              
                HMatrix[Row][Column]    = PopperH + NeighborH


                #---------------------------------------------------------------- IDENTIFY THE PIVOT CELL ASSOCIATED WITH THIS NEIGHBOR
                PivotRow        = PopperRow
                PivotColumn     = PopperColumn
                if   PopperV >  0 and PopperH == 0:                                             # if heading Northward then turning
                    if   NeighborH == -1:                           PivotColumn = PopperColumn - 1  # Westerly,             pivot west  of popper
                    elif NeighborH ==  1:                           PivotColumn = PopperColumn + 1  # Easterly,             pivot east  of popper
                elif PopperV <  0 and PopperH == 0:                                             # if heading Southward then turning
                    if   NeighborH ==  1:                           PivotColumn = PopperColumn + 1  # Easterly,             pivot east  of popper
                    elif NeighborH == -1:                           PivotColumn = PopperColumn - 1  # Westerly,             pivot west  of popper
                elif PopperV == 0 and PopperH >  0:                                             # if heading Eastward then turning
                    if   NeighborV ==  1:                           PivotRow    = PopperRow - 1     # Northerly,            pivot north  of popper
                    elif NeighborV == -1:                           PivotRow    = PopperRow + 1     # Southerly,            pivot south  of popper
                elif PopperV == 0 and PopperH <  0:                                             # if heading Westward then turning
                    if   NeighborV == -1:                           PivotRow    = PopperRow + 1     # Southerly,            pivot south  of popper
                    elif NeighborV ==  1:                           PivotRow    = PopperRow - 1     # Northerly,            pivot north  of popper
                elif PopperV > 0 and PopperH > 0:                                               # if heading Northeasterly then turning
                    if   Direction ==  64:                          PivotColumn = PopperColumn - 1  # North,                pivot west  of popper
                    elif Direction ==   1:                          PivotRow    = PopperRow + 1     # East,                 pivot south of popper  
                    elif Direction == 128 and PopperV/PopperH <  1: PivotRow    = PopperRow - 1     # left  to Northeast,   pivot north of popper
                    elif Direction == 128 and PopperV/PopperH >  1: PivotColumn = PopperColumn + 1  # right to Northeast,   pivot east of popper   
                elif PopperV < 0 and PopperH > 0:                                               # if heading Southeasterly then turning
                    if   Direction ==   1:                          PivotRow    = PopperRow - 1     # East,                 pivot north of popper
                    elif Direction ==   4:                          PivotColumn = PopperColumn - 1  # South,                pivot west  of popper
                    elif Direction ==   2 and PopperV/PopperH < -1: PivotColumn = PopperColumn + 1  # left  to Southeast,   pivot east  of popper
                    elif Direction ==   2 and PopperV/PopperH > -1: PivotRow    = PopperRow + 1     # right to Southeast,   pivot south of popper
                elif PopperV < 0 and PopperH < 0:                                               # if heading Southwesterly then turning
                    if   Direction ==   4:                          PivotColumn = PopperColumn + 1  # South,                pivot east  of popper
                    elif Direction ==  16:                          PivotRow    = PopperRow - 1     # West,                 pivot north of popper 
                    elif Direction ==   8 and PopperV/PopperH <  1: PivotRow    = PopperRow + 1     # left  to Southwest,   pivot south of popper 
                    elif Direction ==   8 and PopperV/PopperH >  1: PivotColumn = PopperColumn - 1  # right to Southwest,   pivot west  of popper  
                elif PopperV > 0 and PopperH < 0:                                               # if heading Northwesterly then turning
                    if   Direction ==  16:                          PivotRow    = PopperRow + 1     # West,                 pivot south of popper
                    elif Direction ==  64:                          PivotColumn = PopperColumn + 1  # North,                pivot east  of popper
                    elif Direction ==  32 and PopperV/PopperH < -1: PivotColumn = PopperColumn - 1  # left  to Northwest,   pivot west  of popper   
                    elif Direction ==  32 and PopperV/PopperH > -1: PivotRow    = PopperRow - 1     # right to Northwest,   pivot north of poppe


                #-------------------------------------------------------------------------------  IF PIVOTING, CANCEL THE ARIADNE EFFECT
                if FrictionMatrix[PivotRow][PivotColumn] > PopperFriction or FrictionMatrix[Row][Column] <> FrictionMatrix[PopperRow][PopperColumn]: 
                    VMatrix[Row][Column]    = NeighborV                     
                    HMatrix[Row][Column]    = NeighborH
                    Distance    = PopperDistance + AddedCost        

                #------------------------------------------- IF NEIGHBOR IS ALREADY AT THE CALCULATED DISTANCE, LEAVE THAT NEIGHBOR ALONE        
                if Distance == NewMatrix[Row][Column]:                        
                    continue
                #---------------------------------------------------------------------- OTHERWISE, ASSIGN CALCULATED DISTANCE TO NEIGHBOR
                else:                                                        
                    NewMatrix[Row][Column]  = Distance                        


                #-------------------------------------------------------------------------------------------  ADD NEIGHBOR TO POPPER LIST
                for OldPopper in PopperList[PopperPosition:]:                 # and go through popper list
                    if Distance >= OldPopper[0]: continue                                           # to determine where
                    PopperList.insert(PopperList.index(OldPopper),[Distance,Row,Column])            # to insert the neighbor
                    break                                                                           # in a distance-sorted
                else:                                                                               # position or at the
                    PopperList.append([Distance,Row,Column])                                        # end of the list

    else: print 'Done with ', len(PopperList), ' cells popped'
    
    return NewMatrix















#------------------------------------------------------------------------------------------------------------------------------  START HERE
if gp.CheckExtension("spatial") == "Available":
    gp.CheckOutExtension("spatial")

    try:
        #---------------------------------------------------------  Check out Spatial Analyst extension license and load required toolboxes        
        gp.CheckOutExtension("Spatial")
        gp.AddToolbox("C:/Program Files/ArcGIS/ArcToolbox/Toolboxes/Spatial Analyst Tools.tbx")
        gp.AddToolbox("C:/Program Files/ArcGIS/ArcToolbox/Toolboxes/Data Management Tools.tbx")
        gp.AddToolbox("C:/Program Files/ArcGIS/ArcToolbox/Toolboxes/Conversion Tools.tbx") 


        #------------------------------------------------------------------------------------- Create a target matrix from an existing grid
        OldTextName     = sys.argv[1] + '_old.asc'
        gp.RasterToASCII_conversion(sys.argv[1],OldTextName)
        OldText         = open(OldTextName,'r').readlines()
        OldMatrix       = []
        for NextLine in OldText[6:]:            
                AllTokens       = NextLine.split()
                NextRow         = []
                if '.' in AllTokens[1]:
                    for NextToken in AllTokens: NextRow.append(float(NextToken))
                else:
                    for NextToken in AllTokens: NextRow.append(int(NextToken))   
                OldMatrix.append(NextRow)
        HowManyRows     = len(OldMatrix)
        HowManyColumns  = len(NextRow)


        #------------------------------------------------------------------------------------ Create a friction matrix from an existing grid
        FrictionTextName     = sys.argv[2] + '_old.asc'
        gp.RasterToASCII_conversion(sys.argv[2],FrictionTextName)
        FrictionText         = open(FrictionTextName,'r').readlines()
        DecimalFound = 0
        FrictionMatrix       = []
        for NextLine in FrictionText[6:]:
                AllTokens       = NextLine.split()
                NextRow         = []
                for NextToken in AllTokens:
                    NextRow.append(float(NextToken))
                FrictionMatrix.append(NextRow)

                
        #---------------------------------------------------------------------------------- Create an elevation matrix from an existing grid
        TopoTextName     = sys.argv[3] + '_old.asc'
        gp.RasterToASCII_conversion(sys.argv[3],TopoTextName)
        TopoText         = open(TopoTextName,'r').readlines()
        DecimalFound = 0
        TopoMatrix       = []
        for NextLine in TopoText[6:]:
                AllTokens       = NextLine.split()
                NextRow         = []
                for NextToken in AllTokens:
                    if '.' in NextToken:
                        DecimalFound = 1
                        break
                    else:
                        NextRow.append(int(NextToken))
                if DecimalFound == 1: break
                TopoMatrix.append(NextRow)           
        if DecimalFound:
            TopoMatrix       = []
            for NextLine in TopoText[6:]:
                AllTokens       = NextLine.split()
                NextRow         = []
                for NextToken in AllTokens:
                    NextRow.append(float(NextToken))
                TopoMatrix.append(NextRow)

        #------------------------------------------------------------------------------------------------ # Inialize some additional matrices
        VMatrix         = []                                                                                
        HMatrix         = []                                                                              
        for R in range(HowManyRows):
            VColumn         = []
            HColumn         = []  
            for C in range(HowManyColumns):
                VColumn.append(0.0)
                HColumn.append(0.0) 
            VMatrix.append(VColumn)                                                                                                                                    
            HMatrix.append(HColumn)                                                             
            

        #----------------------------------------------------------------------------------- Call the SPREAD function to compute a new matrix
        NewMatrix   = OldMatrix                              
        NewMatrix   = spread(HowManyRows,HowManyColumns,OldMatrix,FrictionMatrix,VMatrix,HMatrix,TopoMatrix)


        #----------------------------------------------------------------------------------------------------- Delete some temporary text files
        os.remove(FrictionTextName)
        os.remove(TopoTextName)


        #---------------------------------------------------------------------------------------------- Create output grid from that new matrix
        NewTextName = OldTextName.replace('_old.a', '_new.a')
        NewText     = open(NewTextName ,'a')
        for NextLine in OldText[:6]:
            NewText.writelines(NextLine)
        os.remove(OldTextName)
        for NextRow in range(HowManyRows):
                TextRow = []
                for NextColumn in range(HowManyColumns):
                        TextRow.append(str(NewMatrix[NextRow][NextColumn]) + ' ')
                TextRow.append('\n')
                NewText.writelines(TextRow)
        NewText.close()
        gp.ASCIIToRaster_conversion(NewTextName, sys.argv[4], "FLOAT")
        os.remove(NewTextName)


        #------------------------------------------------------------------------------------------ Check in Spatial Analyst extension license
        gp.CheckInExtension("spatial")        
    except:
        
        #----------------------------------------------------------------------------------------------------- Print error message if necessary
        gp.GetMessages()
else:
    print "Spatial Analyst license is " + gp.CheckExtension("spatial")



    

    




    


    








         